package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.LeaveFeedbackAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CheckPanel: JPanel(BorderLayout()) {
  val middlePanel: JPanel = JPanel(BorderLayout())
  val icon = AsyncProcessIcon("Check in progress")

  init {
    icon.isVisible = false

    add(createButtonToolbar(CheckAction.ACTION_ID), BorderLayout.WEST)
    setDefaultStateForMiddlePanel()

    middlePanel.border = JBUI.Borders.empty(0, 16, 0, 0)
    add(middlePanel, BorderLayout.CENTER)

    val commentAction = ActionManager.getInstance().getAction(LeaveFeedbackAction.ACTION_ID)
    val refreshAction = ActionManager.getInstance().getAction(RefreshTaskFileAction.ACTION_ID)
    val toolbar1 = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(
      commentAction), true)
    toolbar1.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar1.adjustTheSameSize(true)

    val toolbar2 = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(
      refreshAction), true)
    toolbar2.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar2.adjustTheSameSize(true)
    val component2 = toolbar2.component
    component2.border = JBUI.Borders.empty(0, 0, 0, 0)

    val component1 = toolbar1.component
    component1.border = JBUI.Borders.empty(0, 0, 0, 0)

    val actionsPanel = JPanel(HorizontalLayout(0))
    actionsPanel.add(component2)
    actionsPanel.add(component1)
    add(actionsPanel, BorderLayout.EAST)
  }

  private fun createButtonToolbar(actionId: String): JComponent {
    val action = ActionManager.getInstance().getAction(actionId)
    return ActionManager.getInstance().createButtonToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(action))
  }

  fun setDefaultStateForMiddlePanel() {
    middlePanel.removeAll()
    middlePanel.add(icon, BorderLayout.WEST)
    middlePanel.add(JPanel(), BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(middlePanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
  }

  fun checkStarted() {
    setDefaultStateForMiddlePanel()
    icon.isVisible = true
  }

  fun checkFinished(result: CheckResult) {
    icon.isVisible = false
    middlePanel.removeAll()
    val resultPanel = getResultPanel(result)
    middlePanel.add(resultPanel, BorderLayout.WEST)
    middlePanel.add(JPanel(), BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(middlePanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
  }

  private fun getResultPanel(result: CheckResult): JComponent {
    val resultLabel = CheckResultLabel(result)
    if (result.status != CheckStatus.Solved) {
      return resultLabel
    }
    val panel = JPanel(BorderLayout())
    panel.add(createButtonToolbar(NextTaskAction.ACTION_ID), BorderLayout.WEST)
    panel.add(resultLabel, BorderLayout.CENTER)
    return panel
  }
}