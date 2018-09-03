package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.openapi.actionSystem.ActionManager
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
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())

  init {
    add(createButtonToolbar(CheckAction.ACTION_ID), BorderLayout.WEST)
    checkFinishedPanel.border = JBUI.Borders.empty(0, 16, 0, 0)
    add(checkFinishedPanel, BorderLayout.CENTER)
    add(createRightActionsToolbar(), BorderLayout.EAST)
  }

  private fun createRightActionsToolbar(): JPanel {
    val actionsPanel = JPanel(HorizontalLayout(10))
    actionsPanel.add(createSingleActionToolbar(RefreshTaskFileAction.ACTION_ID))
    actionsPanel.add(createSingleActionToolbar(LeaveFeedbackAction.ACTION_ID))
    return actionsPanel
  }

  private fun createButtonToolbar(actionId: String): JComponent {
    val action = ActionManager.getInstance().getAction(actionId)
    return ActionManager.getInstance().createButtonToolbar(ACTION_PLACE, DefaultActionGroup(action))
  }

  private fun createSingleActionToolbar(actionId: String): JComponent {
    val action = ActionManager.getInstance().getAction(actionId)
    val toolbar = ActionManager.getInstance().createActionToolbar(ACTION_PLACE, DefaultActionGroup(action), true)
    //these options affect paddings
    toolbar.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar.adjustTheSameSize(true)

    val component = toolbar.component
    component.border = JBUI.Borders.empty()
    return component
  }

  fun readyToCheck() {
    checkFinishedPanel.removeAll()
  }

  fun checkStarted() {
    checkFinishedPanel.add(AsyncProcessIcon("Check in progress"), BorderLayout.WEST)
  }

  fun checkFinished(result: CheckResult) {
    checkFinishedPanel.removeAll()
    val resultPanel = getResultPanel(result)
    checkFinishedPanel.add(resultPanel, BorderLayout.WEST)
    checkFinishedPanel.add(JPanel(), BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(checkFinishedPanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
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

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}