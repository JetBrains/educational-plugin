package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.api.StepikBaseConnector.Companion.getStepikBaseConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLoginListener
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.Companion.isRemotelyChecked
import com.jetbrains.edu.learning.stepik.hyperskill.showErrorDetails
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls
import java.util.concurrent.TimeUnit

abstract class StepikBaseCheckConnector {
  private val CODE_TASK_CHECK_TIMEOUT = TimeUnit.MINUTES.toSeconds(2)

  protected fun periodicallyCheckSubmissionResult(project: Project, submission: Submission, task: Task): CheckResult {
    require(task.isRemotelyChecked()) { "Task is not checked remotely" }

    val submissionId = submission.id ?: error("Submission must have id")
    val connector = task.getStepikBaseConnector()

    var lastSubmission = submission
    var delay = 1L
    val timeout = if (isUnitTestMode) 5L else CODE_TASK_CHECK_TIMEOUT
    while (delay < timeout && lastSubmission.status == EVALUATION_STATUS) {
      TimeUnit.SECONDS.sleep(delay)
      delay *= 2
      lastSubmission = connector.getSubmission(submissionId).onError { return it.toCheckResult() }
    }

    if (lastSubmission.status != EVALUATION_STATUS) {
      if (task.supportSubmissions()) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.id, lastSubmission)
      }
      return lastSubmission.toCheckResult()
    }

    return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.failed.to.get.check.result.from", connector.platformName))
  }

  abstract fun checkCodeTask(project: Project, task: CodeTask): CheckResult

  fun checkChoiceTask(project: Project, task: ChoiceTask): CheckResult {
    if (!task.isMultipleChoice && task.selectedVariants.isEmpty()) {
      return CheckResult(CheckStatus.Failed, EduCoreBundle.message("choice.task.empty.variant"))
    }

    val checkId = task.checkId()
    if (checkId != null) {
      return checkId
    }
    val submission = StepikBaseSubmitConnector.submitChoiceTask(task).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  fun retryChoiceTask(task: ChoiceTask): Result<Boolean, String> {
    val connector = task.getStepikBaseConnector()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    if (StepikTaskBuilder.fillChoiceTask(attempt, task)) {
      task.selectedVariants.clear()
      return Ok(true)
    }
    return Err(EduCoreBundle.message("hyperskill.choice.task.dataset.empty"))
  }

  companion object {
    @NonNls
    private const val EVALUATION_STATUS: String = "evaluation"

    @JvmStatic
    protected val LOG: Logger = logger<StepikBaseCheckConnector>()

    @JvmStatic
    protected fun String.toCheckResult(): CheckResult {
      return if (this == EduCoreBundle.message("error.access.denied")) {
        CheckResult(CheckStatus.Unchecked,
                    EduCoreBundle.message("error.access.denied.with.link"),
                    hyperlinkListener = HyperskillLoginListener
        )
      }
      else CheckResult(CheckStatus.Unchecked, this)
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

    @JvmStatic
    protected fun Task.checkId(): CheckResult? {
      if (id == 0) {
        val link = feedbackLink ?: return CheckResult.failedToCheck
        val message = """Corrupted task (no id): please, click "Solve in IDE" on <a href="$link">${EduNames.JBA}</a> one more time"""
        return CheckResult(CheckStatus.Unchecked, message)
      }
      return null
    }

    @JvmStatic
    protected fun failedToSubmit(project: Project, task: Task, error: String): CheckResult {
      LOG.error(error)

      val platformName = task.getStepikBaseConnector().platformName
      val message = EduCoreBundle.message("stepik.base.failed.to.submit.task", task.itemType, platformName)

      if (task.course is HyperskillCourse) {
        showErrorDetails(project, error)
      }

      return CheckResult(CheckStatus.Unchecked, message)
    }
  }
}