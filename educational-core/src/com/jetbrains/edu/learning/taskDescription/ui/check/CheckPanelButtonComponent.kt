package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.ide.DataManager
import com.intellij.ide.impl.DataManagerImpl
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JPanel

class CheckPanelButtonComponent(private val action: AnAction,
                                private val isDefault: Boolean = false) : JPanel(GridBagLayout()) {
  init {
    val buttonPanel = JPanel(GridLayout(1, 1, 5, 0))
    val button = object : JButton(action.templatePresentation.text) {
      override fun isDefaultButton(): Boolean = isDefault
    }
    button.addActionListener { e ->
      val event = AnActionEvent(
        null,
        (DataManager.getInstance() as DataManagerImpl).getDataContextTest(this),
        CheckPanel.ACTION_PLACE,
        PresentationFactory().getPresentation(action),
        ActionManager.getInstance(),
        e.modifiers
      )
      ActionUtil.performActionDumbAware(action, event)
    }
    buttonPanel.add(button)
    add(buttonPanel, GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                        Insets(8, 0, 0, 0), 0, 0))
  }
}