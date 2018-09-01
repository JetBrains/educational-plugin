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
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import com.jetbrains.edu.learning.checker.CheckResult
import java.awt.BorderLayout
import javax.swing.JPanel

class CheckPanel: JPanel(BorderLayout()) {
  val middlePanel: JPanel = JPanel(BorderLayout())
  val icon = AsyncProcessIcon("Сheck in progress")

  init {
    icon.isVisible = false
    val action = ActionManager.getInstance().getAction(CheckAction.ACTION_ID)
    val toolbar = ActionManager.getInstance().createButtonToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(action))

    add(toolbar, BorderLayout.WEST)
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
    middlePanel.add(CheckResultLabel(result), BorderLayout.WEST)
    middlePanel.add(JPanel(), BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(middlePanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
  }
}