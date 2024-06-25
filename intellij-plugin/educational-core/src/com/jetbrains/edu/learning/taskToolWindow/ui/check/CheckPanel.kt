package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.*
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.actions.CodeforcesCopyAndSubmitAction
import com.jetbrains.edu.learning.codeforces.actions.SubmitCodeforcesSolutionAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.eduAssistant.utils.isGetHintButtonShown
import com.jetbrains.edu.learning.marketplace.actions.RateMarketplaceCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.stepik.hyperskill.actions.DownloadDatasetAction
import com.jetbrains.edu.learning.stepik.hyperskill.actions.RetryDataTaskAction
import com.jetbrains.edu.learning.stepik.hyperskill.getNextButtonText
import com.jetbrains.edu.learning.taskToolWindow.addActionLinks
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.getUICheckLabel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

class CheckPanel(private val project: Project, private val parentDisposable: Disposable) : JPanel(BorderLayout()) {
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())
  private val checkActionsPanel: JPanel = JPanel(BorderLayout())
  private val linkPanel = JPanel(BorderLayout())
  private val checkDetailsPlaceholder: JPanel = JPanel(BorderLayout())
  private val leftActionsToolbar: JPanel = JPanel(BorderLayout())
  private val checkButtonWrapper = JPanel(BorderLayout())
  private val rightActionsGroup = DefaultActionGroup()
  private val rightActionsToolbar = ActionToolbarImpl(ACTION_PLACE, rightActionsGroup, true)
  private val getHintButtonWrapper = JPanel(BorderLayout())
  private val course = project.course
  private val checkTimeAlarm: Alarm = Alarm(parentDisposable)
  private val asyncProcessIcon = AsyncProcessIcon("Submitting...")

  init {
    rightActionsToolbar.targetComponent = this
    rightActionsGroup.isSearchable = false
    rightActionsToolbar.setActionButtonBorder(2, 0)
    rightActionsToolbar.setMinimumButtonSize(Dimension(28,28))
    rightActionsToolbar.border = JBEmptyBorder(5,0,0,0)

    checkActionsPanel.add(checkButtonWrapper, BorderLayout.WEST)
    leftActionsToolbar.add(checkButtonWrapper, BorderLayout.WEST)
    leftActionsToolbar.add(getHintButtonWrapper, BorderLayout.EAST)
    checkActionsPanel.add(leftActionsToolbar, BorderLayout.WEST)
    checkActionsPanel.add(checkFinishedPanel, BorderLayout.CENTER)
    checkActionsPanel.add(rightActionsToolbar, BorderLayout.EAST)
    checkActionsPanel.add(linkPanel, BorderLayout.NORTH)
    add(checkActionsPanel, BorderLayout.CENTER)
    add(checkDetailsPlaceholder, BorderLayout.NORTH)
    updateRightActionsToolbar()
    asyncProcessIcon.border = JBUI.Borders.empty(8, 6, 0, 10)
  }

  private fun updateRightActionsToolbar(task: Task? = null) {
    rightActionsGroup.removeAll()
    if (task?.isChangedOnFailed != true) {
      rightActionsGroup.add(ActionManager.getInstance().getAction(RevertTaskAction.ACTION_ID))
    }

    if (task != null) {
      val leaveFeedbackActionId = if (task.course.isMarketplace) {
        LeaveInIdeFeedbackAction.ACTION_ID
      }
      else {
        LeaveFeedbackAction.ACTION_ID
      }

      rightActionsGroup.add(ActionManager.getInstance().getAction(leaveFeedbackActionId))

      if (task.course.feedbackLink != null) {
        rightActionsGroup.add(ActionManager.getInstance().getAction(RateMarketplaceCourseAction.ACTION_ID))
      }
    }
  }

  fun readyToCheck() {
    addActionLinks(course, linkPanel, 10, 3)
    checkFinishedPanel.removeAll()
    checkDetailsPlaceholder.removeAll()
    checkTimeAlarm.cancelAllRequests()
  }

  fun checkStarted(startSpinner: Boolean) {
    readyToCheck()
    getHintButtonWrapper.removeAll()
    updateBackground()
    if (startSpinner) {
      checkFinishedPanel.add(asyncProcessIcon, BorderLayout.WEST)
    }
  }

  fun updateCheckDetails(task: Task, result: CheckResult? = null) {
    updateGetHintButtonWrapper(task)
    checkFinishedPanel.removeAll()
    checkFinishedPanel.addNextTaskButton(task)
    checkFinishedPanel.addRetryButton(task)

    val checkResult = result ?: restoreSavedResult(task)
    if (checkResult != null) {
      linkPanel.removeAll()
      checkDetailsPlaceholder.add(CheckDetailsPanel(project, task, checkResult, checkTimeAlarm), BorderLayout.SOUTH)
    }
    updateBackground()
  }

  private fun restoreSavedResult(task: Task): CheckResult? {
    /**
     * We are not showing old result for CheckiO because we store last successful attempt
     * @see com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission.status
     */
    if (task is CheckiOMission) return null
    val feedback = task.feedback
    if (feedback == null) {
      if (task.status == CheckStatus.Unchecked) {
        return null
      }
      return CheckResult(task.status)
    }
    else {
      if (task.isChangedOnFailed && task.status == CheckStatus.Failed) {
        feedback.message = EduCoreBundle.message("action.retry.shuffle.message")
      }
      return feedback.toCheckResult(task.status)
    }
  }

  private fun updateBackground() {
    UIUtil.setBackgroundRecursively(checkFinishedPanel, TaskToolWindowView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkDetailsPlaceholder, TaskToolWindowView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkButtonWrapper, TaskToolWindowView.getTaskDescriptionBackgroundColor())
  }

  fun updateCheckPanel(task: Task) {
    updateCheckButtonWrapper(task)
    updateRightActionsToolbar(task)
    updateCheckDetails(task)
  }

  private fun updateGetHintButtonWrapper(task: Task) {
    getHintButtonWrapper.removeAll()

    if (isGetHintButtonShown(task)) {
      val action = ActionManager.getInstance().getAction(NextStepHintAction.ACTION_ID) as NextStepHintAction
      val nextStepHintButton = CheckPanelButtonComponent(action = action)
      action.actionTargetParent = checkDetailsPlaceholder
      getHintButtonWrapper.add(nextStepHintButton, BorderLayout.WEST)
    }
  }

  private fun updateCheckButtonWrapper(task: Task) {
    checkButtonWrapper.removeAll()
    when (task) {
      is CodeforcesTask -> updateCheckButtonWrapper(task)
      is DataTask -> updateCheckButtonWrapper(task)
      is TheoryTask -> {}
      else -> {
        val isDefault = !(task.isChangedOnFailed && task.status == CheckStatus.Failed || task.isSolved)
        val isEnabled = !(task.isChangedOnFailed && task.status == CheckStatus.Failed)
        val checkComponent = CheckPanelButtonComponent(CheckAction(task.getUICheckLabel()), isDefault = isDefault, isEnabled = isEnabled)
        checkButtonWrapper.add(checkComponent, BorderLayout.WEST)
      }
    }
  }

  private fun updateCheckButtonWrapper(task: CodeforcesTask) {
    var optionalActions: List<AnAction>? = null
    if (CodeforcesSettings.getInstance().isLoggedIn()) {
      optionalActions = listOf(SubmitCodeforcesSolutionAction.ACTION_ID, CodeforcesCopyAndSubmitAction.ACTION_ID).map {
        ActionManager.getInstance().getAction(it) ?: error("Action $it is not found")
      }
    }
    val checkComponent = CheckPanelButtonComponent(project, CheckAction(task.getUICheckLabel()),
                                                   isDefault = false, optionalActions = optionalActions)
    checkButtonWrapper.add(checkComponent, BorderLayout.WEST)
  }

  private fun updateCheckButtonWrapper(task: DataTask) {
    when (task.status) {
      CheckStatus.Unchecked -> {
        val isRunning = task.isRunning()
        val component = if (task.isTimeLimited && isRunning) {
          val endDateTime = task.attempt?.endDateTime ?: error("EndDateTime is expected")
          val checkTimer = CheckTimer(endDateTime) { updateCheckPanel(task) }
          Disposer.register(parentDisposable, checkTimer)
          checkTimer
        }
        else {
          CheckPanelButtonComponent(EduActionUtils.getAction(DownloadDatasetAction.ACTION_ID) as DownloadDatasetAction)
        }
        checkButtonWrapper.add(component, BorderLayout.WEST)

        val checkComponent = CheckPanelButtonComponent(CheckAction(task.getUICheckLabel()), isEnabled = isRunning, isDefault = isRunning)
        checkButtonWrapper.add(checkComponent, BorderLayout.CENTER)
      }
      CheckStatus.Failed, CheckStatus.Solved  -> {
        val retryComponent = CheckPanelButtonComponent(EduActionUtils.getAction(RetryDataTaskAction.ACTION_ID) as RetryDataTaskAction,
                                                       isDefault = true)
        checkButtonWrapper.add(retryComponent, BorderLayout.WEST)
      }
    }
  }

  private fun JPanel.addNextTaskButton(task: Task) {
    if (!(task.status == CheckStatus.Solved
          || task is TheoryTask
          || task.course is HyperskillCourse
          || task.course.courseMode == CourseMode.EDUCATOR)) {
      return
    }

    val nextTask = NavigationUtils.nextTask(task)
    if (nextTask != null || (task.status == CheckStatus.Solved && NavigationUtils.isLastHyperskillProblem(task))) {
      val isDefault = task is TheoryTask || task.isSolved
      val action = ActionManager.getInstance().getAction(NextTaskAction.ACTION_ID)
      val nextButtonText = getNextButtonText(task, nextTask)
      val nextButton = CheckPanelButtonComponent(action = action, isDefault = isDefault, customButtonText = nextButtonText)
      add(nextButton, BorderLayout.WEST)
    }
  }

  private fun getNextButtonText(task: Task, nextTask: Task?): String? {
    nextTask?.let { return EduCoreBundle.message("button.next.task.text", it.presentableName) }
    val course = task.course
    if (course is HyperskillCourse && course.isTaskInTopicsSection(task)) {
      return getNextButtonText(task.id)
    }
    return null
  }

  private fun JPanel.addRetryButton(task: Task) {
    if (!task.isChangedOnFailed) return

    if (task.status == CheckStatus.Failed) {
      val retryComponent = CheckPanelButtonComponent(EduActionUtils.getAction(RetryAction.ACTION_ID) as ActionWithProgressIcon,
        isDefault = true, isEnabled = true)
      add(retryComponent, BorderLayout.WEST)
    }
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}
