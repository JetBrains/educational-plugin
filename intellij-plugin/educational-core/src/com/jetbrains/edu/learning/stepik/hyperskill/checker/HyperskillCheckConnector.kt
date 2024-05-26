package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.notification.NotificationListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.notification.EduErrorNotification
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.api.StepikBasedConnector.Companion.getStepikBasedConnector
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_HOST
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_URL
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLoginListener
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.markStageAsCompleted
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector {
  const val EVALUATION_STATUS = "evaluation"

  private val LOG = Logger.getInstance(HyperskillCheckConnector::class.java)
  private val CODE_TASK_CHECK_TIMEOUT = TimeUnit.MINUTES.toSeconds(2)

  private val loginListener: HyperskillLoginListener
    get() = HyperskillLoginListener

  fun isRemotelyChecked(task: Task): Boolean = when (task) {
    is ChoiceTask -> !task.canCheckLocally
    is CodeTask, is DataTask, is NumberTask, is RemoteEduTask, is StringTask,
    is UnsupportedTask, is SortingTask, is MatchingTask, is TableTask -> true
    else -> false
  }

  private fun periodicallyCheckSubmissionResult(project: Project, submission: StepikBasedSubmission, task: Task): CheckResult {
    require(isRemotelyChecked(task)) { "Task is not checked remotely" }

    val submissionId = submission.id ?: error("Submission must have id")
    val connector = task.getStepikBasedConnector()

    var lastSubmission = submission
    var delay = 1L
    val timeout = if (isUnitTestMode) 5L else CODE_TASK_CHECK_TIMEOUT
    while (delay < timeout && lastSubmission.status == EVALUATION_STATUS) {
      TimeUnit.SECONDS.sleep(delay)
      delay *= 2
      lastSubmission = connector.getSubmission(submissionId).onError { return it.toCheckResult() }
    }

    if (lastSubmission.status != EVALUATION_STATUS) {
      if (task.supportSubmissions) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.id, lastSubmission)
      }
      return lastSubmission.toCheckResult()
    }

    return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.failed.to.get.check.result.from", connector.platformName))
  }

  fun postEduTaskSolution(task: Task, project: Project, result: CheckResult) {
    when (val attemptResponse = HyperskillConnector.getInstance().postAttempt(task)) {
      is Err -> showErrorDetails(project, attemptResponse.error)
      is Ok -> {
        val feedback = if (result.details == null) result.message.xmlUnescaped else "${result.message.xmlUnescaped}\n${result.details}"
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
    val submission = HyperskillSubmissionFactory.createEduTaskSubmission(task, attempt, files, feedback)
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

  private fun checkCodeTaskWithWebSockets(project: Project, task: CodeTask): Result<CheckResult, SubmissionError> {
    val connector = HyperskillConnector.getInstance()
    val webSocketConfiguration = connector.getWebSocketConfiguration().onError { error ->
      return Err(SubmissionError.NoSubmission(error))
    }

    val initialState = InitialState(project, task, webSocketConfiguration.token)
    // TODO: remove `cf_protocol_version=v2` after full transfer to the cf protocol version 2 (~Summer 2023).
    val finalState = connector.connectToWebSocketWithTimeout(CODE_TASK_CHECK_TIMEOUT,
                                                             "wss://${getWebsocketHostName()}/ws/connection/websocket?cf_protocol_version=v2",
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

  fun checkAnswerTask(project: Project, task: AnswerTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    task.validateAnswer(project)?.also {
      return@checkAnswerTask CheckResult(CheckStatus.Failed, it)
    }

    val submission = HyperskillSubmitConnector.submitAnswerTask(project, task).onError { error ->
      return failedToSubmit(project, task, error)
    }

    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  fun checkChoiceTask(project: Project, task: ChoiceTask): CheckResult {
    if (!task.isMultipleChoice && task.selectedVariants.isEmpty()) {
      return CheckResult(CheckStatus.Failed, EduCoreBundle.message("choice.task.empty.variant"))
    }

    val checkId = task.checkId()
    if (checkId != null) {
      return checkId
    }
    val submission = HyperskillSubmitConnector.submitChoiceTask(task).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  fun checkCodeTask(project: Project, task: CodeTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    return checkCodeTaskWithWebSockets(project, task).onError { submissionError ->
      LOG.info(submissionError.error)
      val submission = when (submissionError) {
        is SubmissionError.NoSubmission -> HyperskillSubmitConnector.submitCodeTask(project, task).onError { error ->
          return failedToSubmit(project, task, error)
        }
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
      val connector = task.getStepikBasedConnector()
      return EduCoreBundle.message("error.failed.to.post.solution.to", connector.platformName).toCheckResult()
    }
    val answer = codeExecutor.execute(project, task, indicator).onError {
      return it
    }

    if (answer == DefaultCodeExecutor.NO_OUTPUT) {
      LOG.warn("No output after execution of the `${task.name}` task")
      return EduCoreBundle.message("error.no.output").toCheckResult()
    }

    val submission = HyperskillSubmitConnector.submitDataTask(task, answer).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
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

    val submission = HyperskillSubmitConnector.submitRemoteEduTask(task, files).onError { error ->
      return failedToSubmit(project, task, error)
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

  fun checkUnsupportedTask(task: UnsupportedTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    val connector = task.getStepikBasedConnector()
    val submissions = connector.getSubmissions(task.id)

    if (submissions.isEmpty()) {
      return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("hyperskill.unsupported.check.task.no.submissions"))
    }

    if (submissions.any { it.toCheckResult().isSolved }) {
      return CheckResult.SOLVED
    }

    return submissions.first().toCheckResult()
  }

  fun checkSortingBasedTask(project: Project, task: SortingBasedTask): CheckResult {
    val checkId = task.checkId()
    if (checkId != null) {
      return checkId
    }
    val submission = HyperskillSubmitConnector.submitSortingBasedTask(task).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  fun checkTableTask(project: Project, task: TableTask): CheckResult {
    val checkId = task.checkId()
    if (checkId != null) {
      return checkId
    }
    val submission = HyperskillSubmitConnector.submitTableTask(task).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  fun retryChoiceTask(task: ChoiceTask): Result<Boolean, String> {
    val connector = task.getStepikBasedConnector()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    if (StepikTaskBuilder.fillChoiceTask(attempt, task)) {
      task.selectedVariants.clear()
      return Ok(true)
    }
    return Err(EduCoreBundle.message("hyperskill.choice.task.dataset.empty"))
  }

  private fun showErrorDetails(project: Project, error: String) {
    if (error == EduFormatBundle.message("error.access.denied") || error == EduCoreBundle.message("error.failed.to.refresh.tokens")) {
      EduErrorNotification(
        EduCoreBundle.message("error.failed.to.post.solution"),
        EduCoreBundle.message("error.access.denied.with.link"),
      ).setListener { notification, _ ->
        notification.expire()
        loginListener
      }.notify(project)
      return
    }

    LOG.warn(error)
    EduErrorNotification(
      EduCoreBundle.message("error.failed.to.post.solution"),
      EduFormatBundle.message("help.use.guide", EduNames.FAILED_TO_POST_TO_JBA_URL),
    )
      .setListener(NotificationListener.URL_OPENING_LISTENER)
      .notify(project)
  }

  fun failedToSubmit(project: Project, task: Task, error: String): CheckResult {
    LOG.error(error)

    val platformName = task.getStepikBasedConnector().platformName
    val message = EduCoreBundle.message("stepik.base.failed.to.submit.task", task.itemType, platformName)

    showErrorDetails(project, error)

    return CheckResult(CheckStatus.Unchecked, message)
  }

  fun StepikBasedSubmission.toCheckResult(): CheckResult {
    val status = status ?: return CheckResult.failedToCheck
    val isSolved = status != "wrong"
    var message = hint.nullize() ?: "${StringUtil.capitalize(status)} solution"
    if (isSolved) {
      message = "<html>$message</html>"
    }
    return CheckResult(if (isSolved) CheckStatus.Solved else CheckStatus.Failed, message)
  }

  private fun String.toCheckResult(): CheckResult {
    return if (this == EduFormatBundle.message("error.access.denied")) {
      CheckResult(CheckStatus.Unchecked,
        EduCoreBundle.message("error.access.denied.with.link"),
        hyperlinkAction = { loginListener.doLogin() }
      )
    }
    else CheckResult(CheckStatus.Unchecked, this)
  }

  private fun Task.checkId(): CheckResult? {
    if (id == 0) {
      val link = feedbackLink ?: return CheckResult.failedToCheck
      return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.corrupted.task", link))
    }
    return null
  }
}

