package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor.Companion.NO_OUTPUT
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.Companion.isRemotelyChecked
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory.createEduTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory.createRemoteEduTaskSubmission
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector {
  private val LOG = Logger.getInstance(HyperskillCheckConnector::class.java)
  private val CODE_TASK_CHECK_TIMEOUT = TimeUnit.MINUTES.toSeconds(2)
  const val EVALUATION_STATUS = "evaluation"

  fun postEduTaskSolution(task: Task, project: Project, result: CheckResult) {
    when (val attemptResponse = HyperskillConnector.getInstance().postAttempt(task)) {
      is Err -> showErrorDetails(project, attemptResponse.error)
      is Ok -> {
        val feedback = if (result.details == null) result.message else "${result.message}\n${result.details}"
        postEduSubmission(attemptResponse.value, project, task, feedback)
        checkStageToBeCompleted(task)
      }
    }
  }

  private fun postEduSubmission(attempt: Attempt, project: Project, task: Task, feedback: String) {
    val files = getSolutionFilesResult(project, task).onError { error ->
      showErrorDetails(project, EduCoreBundle.message("error.failed.to.collect.files", task.name))
      LOG.error(error)
      return
    }
    val submission = createEduTaskSubmission(task, attempt, files, feedback)
    when (val response = HyperskillConnector.getInstance().postSubmission(submission)) {
      is Err -> showErrorDetails(project, response.error)
      is Ok -> SubmissionsManager.getInstance(project).addToSubmissionsWithStatus(task.id, task.status, response.value)
    }
  }

  private fun getSolutionFilesResult(project: Project, task: Task): Result<List<SolutionFile>, String> {
    val files = try {
      getSolutionFiles(project, task)
    }
    catch (e: IllegalStateException) {
      return Err("Unable to create submission for the task ${task.name}: ${e.message}")
    }
    return Ok(files)
  }

  private fun String.toCheckResult(): CheckResult {
    return if (this == EduCoreBundle.message("error.access.denied")) {
      CheckResult(CheckStatus.Unchecked,
                  EduCoreBundle.message("error.access.denied.with.link"),
                  hyperlinkListener = HyperskillLoginListener
      )
    }
    else CheckResult(CheckStatus.Unchecked, this)
  }

  fun getLanguage(task: Task): Result<String, String> {
    val course = task.course
    val defaultLanguage = HyperskillLanguages.getLanguageName(course.languageID)
    if (defaultLanguage == null) {
      val languageDisplayName = course.languageDisplayName
      return Err("""Unknown language "$languageDisplayName". Check if support for "$languageDisplayName" is enabled.""")
    }
    return Ok(defaultLanguage)
  }

  fun submitCodeTask(project: Project, task: CodeTask): Result<Submission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = when (val attemptResponse = connector.postAttempt(task)) {
      is Err -> return attemptResponse
      is Ok -> attemptResponse.value
    }
    val defaultLanguage = getLanguage(task).onError {
      return Err(it)
    }

    val configurator = task.course.configurator as? HyperskillConfigurator
    val codeTaskText = configurator?.getCodeTaskFile(project, task)?.getText(project)
    if (codeTaskText == null) {
      LOG.error("Unable to create submission: file with code is not found for the task ${task.name}")
      return Err(EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA))
    }
    val submission = StepikBaseSubmissionFactory.createCodeTaskSubmission(attempt, codeTaskText, defaultLanguage)
    return connector.postSubmission(submission)
  }

  private fun submitDataTask(task: DataTask, answer: String): Result<Submission, String> {
    val attempt = task.attempt ?: return Err("Impossible to submit data task without active attempt")
    val connector = HyperskillConnector.getInstance()
    val submission = StepikBaseSubmissionFactory.createDataTaskSubmission(attempt, answer)
    return connector.postSubmission(submission)
  }

  private fun checkCodeTaskWithWebSockets(project: Project, task: CodeTask): Result<CheckResult, SubmissionError> {
    val connector = HyperskillConnector.getInstance()
    val webSocketConfiguration = connector.getWebSocketConfiguration().onError { error ->
      return Err(SubmissionError.NoSubmission(error))
    }

    val initialState = InitialState(project, task, webSocketConfiguration.token)
    val finalState = connector.connectToWebSocketWithTimeout(CODE_TASK_CHECK_TIMEOUT,
                                                             "wss://${getWebsocketHostName()}/ws/connection/websocket",
                                                             initialState)

    return finalState.getResult()
  }

  private fun getWebsocketHostName(): String {
    return try {
      URL(HYPERSKILL_URL).host
    }
    catch (e: MalformedURLException) {
      return HYPERSKILL_DEFAULT_HOST
    }
  }

  private fun Task.checkId(): CheckResult? {
    if (id == 0) {
      val link = feedbackLink ?: return CheckResult.failedToCheck
      val message = """Corrupted task (no id): please, click "Solve in IDE" on <a href="$link">${EduNames.JBA}</a> one more time"""
      return CheckResult(CheckStatus.Unchecked, message)
    }
    return null
  }

  fun checkCodeTask(project: Project, task: CodeTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    return checkCodeTaskWithWebSockets(project, task).onError { submissionError ->
      LOG.info(submissionError.error)
      val submission = when (submissionError) {
        is SubmissionError.NoSubmission -> submitCodeTask(project, task).onError { return it.toCheckResult() }
        is SubmissionError.WithSubmission -> submissionError.submission
      }

      return periodicallyCheckSubmissionResult(project, submission, task)
    }
  }

  fun checkDataTask(project: Project, task: DataTask, indicator: ProgressIndicator): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    val codeExecutor = task.course.configurator?.taskCheckerProvider?.codeExecutor
    if (codeExecutor == null) {
      LOG.error("Unable to get code executor for the `${task.name}` task")
      return EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA).toCheckResult()
    }
    val answer = codeExecutor.execute(project, task, indicator).onError {
      return it
    }

    if (answer == NO_OUTPUT) {
      LOG.warn("No output after execution of the `${task.name}` task")
      return EduCoreBundle.message("error.no.output").toCheckResult()
    }

    val submission = submitDataTask(task, answer).onError { error ->
      showErrorDetails(project, error)
      return CheckResult.failedToCheck
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  fun checkStringTask(project: Project, task: StringTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }
    if (task.getInputAnswer(project).isBlank()) {
      return CheckResult(CheckStatus.Failed, EduCoreBundle.message("hyperskill.string.task.empty.text"))
    }
    val submissionResult = when (val submissionResponse = submitStringTask(project, task)) {
      is Err -> return CheckResult(CheckStatus.Failed, EduCoreBundle.message("hyperskill.string.task.failed.text"))
      is Ok -> submissionResponse.value
    }

    return periodicallyCheckSubmissionResult(project, submissionResult, task)
  }

  fun submitStringTask(project: Project, task: StringTask): Result<Submission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = when (val attemptResponse = connector.postAttempt(task)) {
      is Err -> return attemptResponse
      is Ok -> attemptResponse.value
    }

    val answer = task.getInputAnswer(project)
    val submission = StepikBaseSubmissionFactory.createStringTaskSubmission(attempt, answer)
    return connector.postSubmission(submission)
  }

  fun checkRemoteEduTask(project: Project, task: RemoteEduTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    val files = getSolutionFilesResult(project, task).onError {
      LOG.error(it)
      return CheckResult.failedToCheck
    }
    val attempt = HyperskillConnector.getInstance().postAttempt(task).onError { error ->
      showErrorDetails(project, error)
      return CheckResult.failedToCheck
    }
    val taskSubmission = createRemoteEduTaskSubmission(task, attempt, files)
    val submission = HyperskillConnector.getInstance().postSubmission(taskSubmission).onError { message ->
      showErrorDetails(project, message)
      return CheckResult.failedToCheck
    }

    val result = periodicallyCheckSubmissionResult(project, submission, task)
    checkStageToBeCompleted(task)
    return result
  }

  private fun checkStageToBeCompleted(task: Task) {
    val course = task.course as HyperskillCourse
    if (course.isTaskInProject(task) && task.status == CheckStatus.Solved) {
      markStageAsCompleted(task)
    }
  }

  private fun periodicallyCheckSubmissionResult(project: Project, submission: Submission, task: Task): CheckResult {
    require(task.isRemotelyChecked()) { "Task is not checked remotely" }

    val submissionId = submission.id!!
    val connector = HyperskillConnector.getInstance()

    var lastSubmission = submission
    var delay = 1L
    val timeout = if (isUnitTestMode) 5L else CODE_TASK_CHECK_TIMEOUT
    while (delay < timeout && lastSubmission.status == EVALUATION_STATUS) {
      TimeUnit.SECONDS.sleep(delay)
      delay *= 2
      lastSubmission = connector.getSubmissionById(submissionId).onError { return it.toCheckResult() }
    }

    if (lastSubmission.status != EVALUATION_STATUS) {
      if (task.supportSubmissions()) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.id, lastSubmission)
      }
      return lastSubmission.toCheckResult()
    }

    return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.failed.to.get.check.result.from", EduNames.JBA))
  }

  fun checkChoiceTask(project: Project, task: ChoiceTask): CheckResult {
    if (!task.isMultipleChoice && task.selectedVariants.isEmpty()) {
      return CheckResult(CheckStatus.Failed, EduCoreBundle.message("hyperskill.choice.task.empty.variant"))
    }

    val checkId = task.checkId()
    if (checkId != null) {
      return checkId
    }
    val submission = submitChoiceTask(task).onError { return it.toCheckResult() }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  private fun submitChoiceTask(task: ChoiceTask): Result<Submission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = when (val attemptResponse = connector.getActiveAttemptOrPostNew(task)) {
      is Err -> return attemptResponse
      is Ok -> attemptResponse.value
    }

    val submission = StepikBaseSubmissionFactory.createChoiceTaskSubmission(task, attempt)
    return connector.postSubmission(submission)
  }

  fun retryChoiceTask(task: ChoiceTask): Result<Boolean, String> {
    val attempt = when (val attemptResponse = HyperskillConnector.getInstance().postAttempt(task)) {
      is Err -> return attemptResponse
      is Ok -> attemptResponse.value
    }

    if (StepikTaskBuilder.fillChoiceTask(attempt, task)) {
      task.selectedVariants.clear()
      return Ok(true)
    }
    return Err(EduCoreBundle.message("hyperskill.choice.task.dataset.empty"))
  }
}

fun Submission.toCheckResult(): CheckResult {
  val status = status ?: return CheckResult.failedToCheck
  val isSolved = status != "wrong"
  var message = hint.nullize() ?: "${StringUtil.capitalize(status)} solution"
  if (isSolved) {
    message = "<html>$message</html>"
  }
  return CheckResult(if (isSolved) CheckStatus.Solved else CheckStatus.Failed, message)
}

