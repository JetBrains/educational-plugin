package com.jetbrains.edu.learning.ui.taskDescription.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
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
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CheckPanel(val project: Project): JPanel(BorderLayout()) {
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())
  private val checkActionsPanel: JPanel = JPanel(BorderLayout())
  private val checkDetailsPlaceholder: JPanel = JPanel(BorderLayout())

  init {
    checkActionsPanel.add(createButtonToolbar(CheckAction.ACTION_ID), BorderLayout.WEST)
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
    checkDetailsPlaceholder.removeAll()
  }

  fun checkStarted() {
    readyToCheck()
    val asyncProcessIcon = AsyncProcessIcon("Check in progress")
    asyncProcessIcon.border = JBUI.Borders.empty(0, 16, 0, 0)
    checkFinishedPanel.add(asyncProcessIcon, BorderLayout.WEST)
  }

  fun checkFinished(result: CheckResult) {
    checkFinishedPanel.removeAll()
    val resultPanel = getResultPanel(result)
    checkFinishedPanel.add(resultPanel, BorderLayout.WEST)
    checkFinishedPanel.add(JPanel(), BorderLayout.CENTER)
    checkDetailsPlaceholder.add(CheckDetailsPanel(project, result))
    UIUtil.setBackgroundRecursively(this, EditorColorsManager.getInstance().globalScheme.defaultBackground)
  }

  private fun getResultPanel(result: CheckResult): JComponent {
    val resultLabel = CheckResultLabel(result)
    if (result.status != CheckStatus.Solved) {
      return resultLabel
    }
    val panel = JPanel(BorderLayout())
    val nextButton = createButtonToolbar(NextTaskAction.ACTION_ID)
    nextButton.border = JBUI.Borders.empty(0, 12, 0, 0)
    panel.add(nextButton, BorderLayout.WEST)
    panel.add(resultLabel, BorderLayout.CENTER)
    return panel
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}