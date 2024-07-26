package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBOptionButton
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.containers.headTail
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.ui.isDefault
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Panel with button inside and progress icon if action is appropriate
 */
class CheckPanelButtonComponent private constructor() : JPanel(BorderLayout()) {
  /**
   * @param[action] action to execute when button is clicked. Panel will also have process icon when action is being executed.
   * @param[isDefault] parameter specifies whether button is painted as default or not. `false` by default.
   * @param[isEnabled] parameter for enabling/disabling button. `true` by default.
   *
   * @see com.jetbrains.edu.learning.actions.ActionWithProgressIcon
   */
  constructor(action: ActionWithProgressIcon, isDefault: Boolean = false, isEnabled: Boolean = true) : this() {
    val buttonPanel = createButtonPanel(action = action, isDefault = isDefault, isEnabled = isEnabled)
    add(buttonPanel, BorderLayout.WEST)

    val spinnerPanel = action.spinnerPanel
    if (spinnerPanel != null) {
      add(spinnerPanel, BorderLayout.CENTER)
    }
  }

  /**
   * @param[action] action to execute when button is clicked.
   * @param[isDefault] parameter specifies whether button is painted as default or not. `false` by default.
   * @param[isEnabled] parameter for enabling/disabling button. `true` by default.
   */
  constructor(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    customButtonText: String? = null
  ) : this() {
    val buttonPanel = createButtonPanel(action, isDefault = isDefault, isEnabled = isEnabled, customButtonText = customButtonText)
    add(buttonPanel)
  }

  private fun createButtonPanel(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    customButtonText: String? = null
  ): JPanel {
    val button = createButton(action, isDefault = isDefault, isEnabled = isEnabled, customButtonText = customButtonText)
    return Wrapper(button)
  }

  private fun createButton(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    customButtonText: String? = null
  ): JButton {
    val text = if (customButtonText != null) StringUtils.abbreviate(customButtonText, 25) else action.templatePresentation.text
    val button =  JButton(text).apply {
      this.isEnabled = isEnabled
      this.isFocusable = isEnabled
      this.isDefault = isDefault
    }
    if (isEnabled) {
      button.addActionListener { e ->
        performAnAction(e, this, action)
      }
    }
    return button
  }


  private inner class DefaultOptionalButton(
    mainAction: AnAction,
    otherActions: List<AnAction>
  ) : JBOptionButton(AnActionWrapper(mainAction, this@CheckPanelButtonComponent), null) {

    init {
      setOptions(otherActions)
    }

    override fun isDefaultButton() = true
  }

  private fun createDefaultOptionalButton(optionalActions: List<AnAction>?,
                                          project: Project?): DefaultOptionalButton? {
    if (!optionalActions.isNullOrEmpty() && project != null) {
      val (mainAction, otherActions) = optionalActions.headTail()

      val optionButton = DefaultOptionalButton(mainAction, otherActions)
      return optionButton
    }
    return null
  }
}

private class AnActionWrapper(
  private val action: AnAction,
  private val component: JComponent
) : AbstractAction(action.templatePresentation.text) {
  override fun actionPerformed(e: ActionEvent) {
    performAnAction(e, component, action)
  }
}

private fun performAnAction(actionEvent: ActionEvent, component: JComponent, action: AnAction) {
  val dataContext = DataManager.getInstance().getDataContext(component)
  val event = AnActionEvent(
    null,
    dataContext,
    CheckPanel.ACTION_PLACE,
    PresentationFactory().getPresentation(action),
    ActionManager.getInstance(),
    actionEvent.modifiers
  )

  ActionUtil.performActionDumbAwareWithCallbacks(action, event)
}