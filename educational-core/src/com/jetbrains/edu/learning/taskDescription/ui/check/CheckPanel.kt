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
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.GoToTaskUrlAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.OPEN_ON_CODEFORCES_ACTION
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
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
    } else {
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

  fun checkFinished(task: Task, result: CheckResult) {
    checkFinishedPanel.removeAll()
    val resultPanel = getResultPanel(task, result)
    checkFinishedPanel.add(resultPanel, BorderLayout.WEST)
    checkFinishedPanel.add(JPanel(), BorderLayout.CENTER)
    checkDetailsPlaceholder.add(CheckDetailsPanel(project, task, result))
    updateBackground()
  }

  private fun updateBackground() {
    UIUtil.setBackgroundRecursively(checkFinishedPanel, TaskDescriptionView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkDetailsPlaceholder, TaskDescriptionView.getTaskDescriptionBackgroundColor())
  }

  private fun getResultPanel(task: Task, result: CheckResult): JComponent {
    val resultLabel = CheckResultLabel(task, result)
    if (result.status != CheckStatus.Solved) {
      return resultLabel
    }
    val panel = createNextTaskButtonPanel(task)
    panel.add(resultLabel, BorderLayout.CENTER)
    return panel
  }

  private fun createNextTaskButtonPanel(task: Task): JPanel {
    val panel = JPanel(BorderLayout())
    if (NavigationUtils.nextTask(task) != null) {
      val nextButton = CheckPanelButtonComponent(ActionManager.getInstance().getAction(NextTaskAction.ACTION_ID))
      nextButton.border = JBUI.Borders.empty(0, 12, 0, 0)
      panel.add(nextButton, BorderLayout.WEST)
    }
    return panel
  }

  private fun addResultPanel(task: Task) {
    checkFinishedPanel.add(createNextTaskButtonPanel(task))
  }

  fun updateCheckPanel(task: Task) {
    updateCheckButtonWrapper(task)
    updateRightActionsToolbar()
  }

  private fun updateCheckButtonWrapper(task: Task) {
    checkButtonWrapper.removeAll()
    checkButtonWrapper.add(CheckPanelButtonComponent(CheckAction.createCheckAction(task), true), BorderLayout.WEST)
    if (task is TheoryTask || task.status == CheckStatus.Solved) {
      addResultPanel(task)
    }
  }

  private fun updateRightActionsToolbar() {
    rightActionsToolbar.removeAll()
    createRightActionsToolbar()
  }

  fun checkTooltipPosition(): RelativePoint {
    return JBPopupFactory.getInstance().guessBestPopupLocation(checkButtonWrapper)
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}