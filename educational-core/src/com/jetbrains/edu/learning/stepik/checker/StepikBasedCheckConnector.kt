package com.jetbrains.edu.learning.stepik.checker

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.HyperlinkAdapter
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.api.StepikBasedConnector.Companion.getStepikBasedConnector
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.checker.StepikCheckConnector.showErrorDetails
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls
import java.util.concurrent.TimeUnit

abstract class StepikBasedCheckConnector {
  private val CODE_TASK_CHECK_TIMEOUT = TimeUnit.MINUTES.toSeconds(2)

  protected abstract val loginListener: StepikBasedLoginListener
  protected abstract val linkToHelp: String

  protected open val remotelyCheckedTasks: Set<Class<out Task>> = setOf(
    ChoiceTask::class.java,
    CodeTask::class.java,
    DataTask::class.java,
    NumberTask::class.java,
    StringTask::class.java
  )

  fun isRemotelyChecked(task: Task): Boolean = when (task) {
    is ChoiceTask -> !task.canCheckLocally
    else -> task.javaClass in remotelyCheckedTasks
  }

  protected fun periodicallyCheckSubmissionResult(project: Project, submission: StepikBasedSubmission, task: Task): CheckResult {
    require(isRemotelyChecked(task)) { "Task is not checked remotely" }

    val submissionId = submission.id ?: error("Submission must have id")
    val connector = task.getStepikBasedConnector()

    var lastSubmission = submission
    var delay = 1L
    val timeout = if (isUnitTestMode) 5L else CODE_TASK_CHECK_TIMEOUT
    while (delay < timeout && lastSubmission.status == EVALUATION_STATUS) {
      TimeUnit.SECONDS.sleep(delay)
      delay *= 2
      lastSubmission = connector.getSubmission(submissionId).onError { return it.toCheckResult(loginListener) }
    }

    if (lastSubmission.status != EVALUATION_STATUS) {
      if (task.supportSubmissions) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.id, lastSubmission)
      }
      return lastSubmission.toCheckResult()
    }

    return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.failed.to.get.check.result.from", connector.platformName))
  }

  fun checkAnswerTask(project: Project, task: AnswerTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    task.validateAnswer(project)?.also {
      return@checkAnswerTask CheckResult(CheckStatus.Failed, it)
    }

    val submission = StepikBasedSubmitConnector.submitAnswerTask(project, task).onError { error ->
      return failedToSubmit(project, task, error)
    }

    return periodicallyCheckSubmissionResult(project, submission, task)
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
    val submission = StepikBasedSubmitConnector.submitChoiceTask(task).onError { error ->
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

  fun checkDataTask(project: Project, task: DataTask, indicator: ProgressIndicator): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    val codeExecutor = task.course.configurator?.taskCheckerProvider?.codeExecutor
    if (codeExecutor == null) {
      LOG.error("Unable to get code executor for the `${task.name}` task")
      val connector = task.getStepikBasedConnector()
      return EduCoreBundle.message("error.failed.to.post.solution.to", connector.platformName).toCheckResult(loginListener)
    }
    val answer = codeExecutor.execute(project, task, indicator).onError {
      return it
    }

    if (answer == DefaultCodeExecutor.NO_OUTPUT) {
      LOG.warn("No output after execution of the `${task.name}` task")
      return EduCoreBundle.message("error.no.output").toCheckResult(loginListener)
    }

    val submission = StepikBasedSubmitConnector.submitDataTask(task, answer).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }

  protected fun showErrorDetails(project: Project, error: String) {
    if (error == EduCoreBundle.message("error.access.denied") || error == EduCoreBundle.message("error.failed.to.refresh.tokens")) {
      Notification(
        "JetBrains Academy",
        EduCoreBundle.message("error.failed.to.post.solution"),
        EduCoreBundle.message("error.access.denied.with.link"),
        NotificationType.ERROR
      ).setListener { notification, _ ->
        notification.expire()
        loginListener
      }.notify(project)
      return
    }

    LOG.warn(error)
    Notification(
      "JetBrains Academy",
      EduCoreBundle.message("error.failed.to.post.solution"),
      EduFormatBundle.message("help.use.guide", linkToHelp),
      NotificationType.ERROR
    )
      .setListener(NotificationListener.URL_OPENING_LISTENER)
      .notify(project)
}

  companion object {
    @NonNls
    private const val EVALUATION_STATUS: String = "evaluation"

    @JvmStatic
    protected val LOG: Logger = logger<StepikBasedCheckConnector>()

    @JvmStatic
    protected fun String.toCheckResult(loginListener: StepikBasedLoginListener): CheckResult {
      return if (this == EduCoreBundle.message("error.access.denied")) {
        CheckResult(CheckStatus.Unchecked,
                    EduCoreBundle.message("error.access.denied.with.link"),
                    hyperlinkAction = { loginListener.doLogin() }
        )
      }
      else CheckResult(CheckStatus.Unchecked, this)
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

    @JvmStatic
    protected fun Task.checkId(): CheckResult? {
      if (id == 0) {
        val link = feedbackLink ?: return CheckResult.failedToCheck
        return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.corrupted.task", link))
      }
      return null
    }

    @JvmStatic
    fun failedToSubmit(project: Project, task: Task, error: String): CheckResult {
      LOG.error(error)

      val platformName = task.getStepikBasedConnector().platformName
      val message = EduCoreBundle.message("stepik.base.failed.to.submit.task", task.itemType, platformName)

      showErrorDetails(project, error)

      return CheckResult(CheckStatus.Unchecked, message)
    }
  }
}

abstract class StepikBasedLoginListener: HyperlinkAdapter() {
  abstract fun doLogin()
}