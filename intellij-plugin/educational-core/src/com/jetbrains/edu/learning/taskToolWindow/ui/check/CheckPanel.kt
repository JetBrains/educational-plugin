package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.Alarm
import com.intellij.util.ui.AsyncProcessIcon
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
import com.jetbrains.edu.learning.marketplace.actions.RateMarketplaceCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.stepik.hyperskill.actions.DownloadDatasetAction
import com.jetbrains.edu.learning.stepik.hyperskill.actions.RetryDataTaskAction
import com.jetbrains.edu.learning.taskToolWindow.addActionLinks
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.retry.RetryHyperlinkComponent
import com.jetbrains.edu.learning.ui.getUICheckLabel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CheckPanel(private val project: Project, private val parentDisposable: Disposable) : JPanel(BorderLayout()) {
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())
  private val checkActionsPanel: JPanel = JPanel(BorderLayout())
  private val linkPanel = JPanel(BorderLayout())
  private val checkDetailsPlaceholder: JPanel = JPanel(BorderLayout())
  private val checkButtonWrapper = JPanel(BorderLayout())
  private val rightActionsToolbar = JPanel(HorizontalLayout(10))
  private val course = project.course
  private val checkTimeAlarm: Alarm = Alarm(parentDisposable)
  private val asyncProcessIcon = AsyncProcessIcon("Submitting...")

  init {
    checkActionsPanel.add(checkButtonWrapper, BorderLayout.WEST)
    checkActionsPanel.add(checkFinishedPanel, BorderLayout.CENTER)
    checkActionsPanel.add(createRightActionsToolbar(), BorderLayout.EAST)
    checkActionsPanel.add(linkPanel, BorderLayout.NORTH)
    add(checkActionsPanel, BorderLayout.CENTER)
    add(checkDetailsPlaceholder, BorderLayout.NORTH)
    asyncProcessIcon.border = JBUI.Borders.empty(8, 6, 0, 10)
  }

  private fun createRightActionsToolbar(task: Task? = null): JPanel {
    if (task?.isChangedOnFailed != true) {
      rightActionsToolbar.add(createSingleActionToolbar(RevertTaskAction.ACTION_ID))
    }

    if (task != null) {
      val leaveFeedbackActionId = if (task.course.isMarketplace) {
        LeaveInIdeFeedbackAction.ACTION_ID
      }
      else {
        LeaveFeedbackAction.ACTION_ID
      }

      rightActionsToolbar.add(createSingleActionToolbar(leaveFeedbackActionId))

      if (task.course.feedbackLink != null) {
        rightActionsToolbar.add(createSingleActionToolbar(RateMarketplaceCourseAction.ACTION_ID))
      }
    }
    return rightActionsToolbar
  }

  private fun createSingleActionToolbar(actionId: String): JComponent {
    val action = ActionManager.getInstance().getAction(actionId)
    return createSingleActionToolbar(action)
  }

  private fun createSingleActionToolbar(action: AnAction): JComponent {
    val toolbar = ActionManager.getInstance().createActionToolbar(ACTION_PLACE, DefaultActionGroup(action), true)
    //these options affect paddings
    toolbar.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar.adjustTheSameSize(true)
    toolbar.targetComponent = this

    val component = toolbar.component
    component.border = JBUI.Borders.emptyTop(5)
    return component
  }

  fun readyToCheck() {
    addActionLinks(course, linkPanel, 10, 3)
    checkFinishedPanel.removeAll()
    checkDetailsPlaceholder.removeAll()
    checkTimeAlarm.cancelAllRequests()
  }

  fun checkStarted(startSpinner: Boolean) {
    readyToCheck()
    updateBackground()
    if (startSpinner) {
      checkFinishedPanel.add(asyncProcessIcon, BorderLayout.WEST)
    }
  }

  fun updateCheckDetails(task: Task, result: CheckResult? = null) {
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
  }

  fun updateCheckPanel(task: Task) {
    updateCheckButtonWrapper(task)
    updateRightActionsToolbar(task)
    updateCheckDetails(task)
  }

  private fun updateCheckButtonWrapper(task: Task) {
    checkButtonWrapper.removeAll()
    when (task) {
      is CodeforcesTask -> updateCheckButtonWrapper(task)
      is DataTask -> updateCheckButtonWrapper(task)
      is TheoryTask -> {}
      else -> {
        val isDefault = !(task.isChangedOnFailed && task.status == CheckStatus.Failed || task.isSolved)
        val checkComponent = CheckPanelButtonComponent(CheckAction(task.getUICheckLabel()), isDefault = isDefault, isEnabled = isDefault)
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

  private fun updateRightActionsToolbar(task: Task) {
    rightActionsToolbar.removeAll()
    createRightActionsToolbar(task)
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
      val nextButtonText = nextTask?.let {  EduCoreBundle.message("button.next.task.text", nextTask.name) }
      val nextButton = CheckPanelButtonComponent(action = action, isDefault = isDefault, customButtonText = nextButtonText)
      add(nextButton, BorderLayout.WEST)
    }
  }

  private fun JPanel.addRetryButton(task: Task) {
    if (!task.isChangedOnFailed) return

    if (task.status == CheckStatus.Failed) {
      val retryLink = RetryHyperlinkComponent(EduCoreBundle.message("action.retry.try.again.description"),
                                              ActionManager.getInstance().getAction(RetryAction.ACTION_ID) as ActionWithProgressIcon)
      add(retryLink, BorderLayout.WEST)
    }
  }

  fun checkTooltipPosition(): RelativePoint {
    return JBPopupFactory.getInstance().guessBestPopupLocation(checkButtonWrapper)
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}
