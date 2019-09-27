package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.LeaveFeedbackAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CheckPanel(val project: Project): JPanel(BorderLayout()) {
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())
  private val checkActionsPanel: JPanel = JPanel(BorderLayout())
  private val checkDetailsPlaceholder: JPanel = JPanel(BorderLayout())
  private val checkButtonWrapper = JPanel(BorderLayout())

  init {
    checkActionsPanel.add(checkButtonWrapper, BorderLayout.WEST)
    checkActionsPanel.add(checkFinishedPanel, BorderLayout.CENTER)
    checkActionsPanel.add(createRightActionsToolbar(), BorderLayout.EAST)
    add(checkActionsPanel, BorderLayout.CENTER)
    add(checkDetailsPlaceholder, BorderLayout.SOUTH)
  }

  private fun createRightActionsToolbar(): JPanel {
    val actionsPanel = JPanel(HorizontalLayout(10))
    actionsPanel.add(createSingleActionToolbar(RevertTaskAction.ACTION_ID))
    actionsPanel.add(createSingleActionToolbar(LeaveFeedbackAction.ACTION_ID))
    return actionsPanel
  }

  private fun createButtonToolbar(actionId: String): JComponent {
    val action = ActionManager.getInstance().getAction(actionId)
    return createButtonToolbar(action)
  }

  private fun createButtonToolbar(action: AnAction) =
    ActionManager.getInstance().createButtonToolbar(ACTION_PLACE, DefaultActionGroup(action))

  private fun createSingleActionToolbar(actionId: String): JComponent {
    val action = ActionManager.getInstance().getAction(actionId)
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
    UIUtil.setBackgroundRecursively(this, TaskDescriptionView.getTaskDescriptionBackgroundColor())
  }

  private fun getResultPanel(task: Task, result: CheckResult): JComponent {
    val resultLabel = CheckResultLabel(task, result)
    if (result.status != CheckStatus.Solved) {
      return resultLabel
    }
    val panel = createResultPanel(task)
    panel.add(resultLabel, BorderLayout.CENTER)
    return panel
  }

  private fun createResultPanel(task: Task): JPanel {
    val panel = JPanel(BorderLayout())
    if (NavigationUtils.nextTask(task) != null) {
      val nextButton = createButtonToolbar(NextTaskAction.ACTION_ID)
      nextButton.border = JBUI.Borders.empty(0, 12, 0, 0)
      panel.add(nextButton, BorderLayout.WEST)
    }
    return panel
  }

  private fun addResultPanel(task: Task) {
    checkFinishedPanel.add(createResultPanel(task))
  }

  fun updateCheckPanel(task: Task) {
    checkButtonWrapper.removeAll()
    checkButtonWrapper.add(createButtonToolbar(CheckAction.createCheckAction(task)), BorderLayout.WEST)
    if (task is TheoryTask) {
      addResultPanel(task)
    }
  }

  fun checkTooltipPosition(): RelativePoint {
    return JBPopupFactory.getInstance().guessBestPopupLocation(checkButtonWrapper)
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}