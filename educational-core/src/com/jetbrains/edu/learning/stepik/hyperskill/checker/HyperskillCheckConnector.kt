package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor.Companion.NO_OUTPUT
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.stepik.checker.StepikBaseCheckConnector
import com.jetbrains.edu.learning.stepik.checker.StepikBaseSubmitConnector
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector : StepikBaseCheckConnector() {
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
    val submission = StepikBaseSubmissionFactory.createEduTaskSubmission(task, attempt, files, feedback)
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

  override fun checkCodeTask(project: Project, task: CodeTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    return checkCodeTaskWithWebSockets(project, task).onError { submissionError ->
      LOG.info(submissionError.error)
      val submission = when (submissionError) {
        is SubmissionError.NoSubmission -> StepikBaseSubmitConnector.submitCodeTask(project, task).onError { error ->
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
      return EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA).toCheckResult()
    }
    val answer = codeExecutor.execute(project, task, indicator).onError {
      return it
    }

    if (answer == NO_OUTPUT) {
      LOG.warn("No output after execution of the `${task.name}` task")
      return EduCoreBundle.message("error.no.output").toCheckResult()
    }

    val submission = StepikBaseSubmitConnector.submitDataTask(task, answer).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  fun checkAnswerTask(project: Project, task: AnswerTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    task.validateAnswer(project)?.also {
      return@checkAnswerTask CheckResult(CheckStatus.Failed, it)
    }

    val submission = StepikBaseSubmitConnector.submitAnswerTask(project, task).onError { error ->
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

    val submission = StepikBaseSubmitConnector.submitRemoteEduTask(task, files).onError { error ->
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
}

