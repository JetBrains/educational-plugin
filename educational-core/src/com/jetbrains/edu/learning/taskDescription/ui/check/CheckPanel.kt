package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.ActionLink
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.Alarm
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.GoToTaskUrlAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.OPEN_ON_CODEFORCES_ACTION
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CheckPanel(val project: Project) : JPanel(BorderLayout()) {
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())
  private val checkActionsPanel: JPanel = JPanel(BorderLayout())
  private val checkDetailsPlaceholder: JPanel = JPanel(BorderLayout())
  private val checkButtonWrapper = JPanel(BorderLayout())
  private val rightActionsToolbar = JPanel(HorizontalLayout(10))
  private val task = EduUtils.getCurrentTask(project)
  private val checkTimeAlarm: Alarm = Alarm(project)

  init {
    checkActionsPanel.add(checkButtonWrapper, BorderLayout.WEST)
    checkActionsPanel.add(checkFinishedPanel, BorderLayout.CENTER)
    checkActionsPanel.add(createRightActionsToolbar(), BorderLayout.EAST)
    add(checkActionsPanel, BorderLayout.CENTER)
    add(checkDetailsPlaceholder, BorderLayout.SOUTH)
  }

  private fun createRightActionsToolbar(): JPanel {
    if (task is CodeforcesTask) {
      rightActionsToolbar.add(createActionLink(OPEN_ON_CODEFORCES_ACTION, GoToTaskUrlAction.ACTION_ID))
    }
    else {
      rightActionsToolbar.add(createSingleActionToolbar(RevertTaskAction.ACTION_ID))
      rightActionsToolbar.add(createSingleActionToolbar(GoToTaskUrlAction.ACTION_ID))
    }
    return rightActionsToolbar
  }

  private fun createSingleActionToolbar(actionId: String): JComponent {
    val action = ActionManager.getInstance().getAction(actionId)
    return createSingleActionToolbar(action)
  }

  @Suppress("SameParameterValue")
  private fun createActionLink(actionText: String, actionId: String): ActionLink {
    return ActionLink(actionText, ActionManager.getInstance().getAction(actionId))
  }

  private fun createSingleActionToolbar(action: AnAction): JComponent {
    val toolbar = ActionManager.getInstance().createActionToolbar(ACTION_PLACE, DefaultActionGroup(action), true)
    //these options affect paddings
    toolbar.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar.adjustTheSameSize(true)

    val component = toolbar.component
    component.border = JBUI.Borders.empty(5, 0, 0, 0)
    return component
  }

  fun readyToCheck() {
    checkFinishedPanel.removeAll()
    checkDetailsPlaceholder.removeAll()
    checkTimeAlarm.cancelAllRequests()
  }

  fun checkStarted() {
    readyToCheck()
    val asyncProcessIcon = AsyncProcessIcon("Check in progress")
    val iconPanel = JPanel(BorderLayout())
    iconPanel.add(asyncProcessIcon, BorderLayout.CENTER)
    iconPanel.border = JBUI.Borders.empty(8, 16, 0, 0)
    checkFinishedPanel.add(iconPanel, BorderLayout.WEST)
    updateBackground()
  }

  fun updateCheckDetails(task: Task, result: CheckResult? = null) {
    checkFinishedPanel.removeAll()
    checkFinishedPanel.addNextTaskButton(task)

    val checkResult = result ?: restoreSavedResult(task)
    if (checkResult != null) {
      checkDetailsPlaceholder.add(CheckDetailsPanel(project, task, checkResult, checkTimeAlarm), BorderLayout.SOUTH)
    }
    updateBackground()
  }

  private fun restoreSavedResult(task: Task): CheckResult? {
    /**
     * We are not showing old result for CheckiO because we store last successful attempt
     * @see com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission.setStatus
     */
    if (task is CheckiOMission) return null
    if (task.feedback == null && task.status == CheckStatus.Unchecked) return null

    val feedback = task.feedback ?: return CheckResult(task.status, "")
    return feedback.toCheckResult(task.status)
  }

  private fun updateBackground() {
    UIUtil.setBackgroundRecursively(checkFinishedPanel, TaskDescriptionView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkDetailsPlaceholder, TaskDescriptionView.getTaskDescriptionBackgroundColor())
  }

  fun updateCheckPanel(task: Task) {
    updateCheckButtonWrapper(task)
    updateRightActionsToolbar()
    updateCheckDetails(task)
  }

  private fun updateCheckButtonWrapper(task: Task) {
    checkButtonWrapper.removeAll()
    checkButtonWrapper.add(CheckPanelButtonComponent(CheckAction.createCheckAction(task), true), BorderLayout.WEST)
    checkFinishedPanel.addNextTaskButton(task)
  }

  private fun updateRightActionsToolbar() {
    rightActionsToolbar.removeAll()
    createRightActionsToolbar()
  }

  private fun JPanel.addNextTaskButton(task: Task) {
    if ((task.status == CheckStatus.Solved || task is TheoryTask || task.course is HyperskillCourse) &&
        NavigationUtils.nextTask(task) != null) {
      val nextButton = CheckPanelButtonComponent(ActionManager.getInstance().getAction(NextTaskAction.ACTION_ID))
      nextButton.border = JBUI.Borders.empty(0, 12, 0, 0)
      add(nextButton, BorderLayout.WEST)
    }
  }

  fun checkTooltipPosition(): RelativePoint {
    return JBPopupFactory.getInstance().guessBestPopupLocation(checkButtonWrapper)
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}