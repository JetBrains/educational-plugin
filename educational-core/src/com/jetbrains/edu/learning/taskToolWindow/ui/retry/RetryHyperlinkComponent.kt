package com.jetbrains.edu.learning.taskToolWindow.ui.retry

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.ui.HyperlinkLabel
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanelButtonComponent.Companion.createButtonPanel
import java.awt.*
import javax.swing.JPanel

class RetryHyperlinkComponent private constructor() : JPanel(BorderLayout()) {

  constructor(text: String, action: ActionWithProgressIcon) : this() {
    val panel = createHyperlinkPanel(text, action)
    add(panel, BorderLayout.WEST)

    val spinnerPanel = action.spinnerPanel
    if (spinnerPanel != null) {
      add(spinnerPanel, BorderLayout.CENTER)
    }
  }

  private fun createHyperlinkPanel(text: String, action: AnAction): JPanel {
    val retryLink = HyperlinkLabel(text)

    retryLink.addHyperlinkListener {
      val dataContext = DataManager.getInstance().getDataContext(this)
      val event = AnActionEvent.createFromAnAction(action,
                                                   it.inputEvent,
                                                   CheckPanel.ACTION_PLACE,
                                                   dataContext)
      ActionUtil.performActionDumbAwareWithCallbacks(action, event)
    }

    return createButtonPanel(retryLink)
  }
}