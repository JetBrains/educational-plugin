package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.checker.StepikBasedCheckConnector
import com.jetbrains.edu.learning.stepik.checker.StepikBasedSubmitConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_HOST
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_URL
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.markStageAsCompleted
import com.jetbrains.edu.learning.stepik.hyperskill.showErrorDetails
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector : StepikBasedCheckConnector() {
  private val LOG = Logger.getInstance(HyperskillCheckConnector::class.java)
  private val CODE_TASK_CHECK_TIMEOUT = TimeUnit.MINUTES.toSeconds(2)
  const val EVALUATION_STATUS = "evaluation"

  override val remotelyCheckedTasks: Set<Class<out Task>>
    get() = setOf(
      *super.remotelyCheckedTasks.toTypedArray(),
      RemoteEduTask::class.java,
    )

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
    val submission = StepikBasedSubmissionFactory.createEduTaskSubmission(task, attempt, files, feedback)
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
        is SubmissionError.NoSubmission -> StepikBasedSubmitConnector.submitCodeTask(project, task).onError { error ->
          return failedToSubmit(project, task, error)
        }
        is SubmissionError.WithSubmission -> submissionError.submission
      }

      return periodicallyCheckSubmissionResult(project, submission, task)
    }
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

    val submission = StepikBasedSubmitConnector.submitRemoteEduTask(task, files).onError { error ->
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

