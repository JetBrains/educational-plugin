package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import javax.swing.JButton
import javax.swing.JComponent

class PrimaryButtonZhabaAction : ZhabaCustomComponentAction() {
  override fun createButton(): JButton = JButton().apply {
    isFocusable = false
    isOpaque = false
    toolTipText = null

    // This makes the button look exactly like the button inside the GotIt tooltip
    putClientProperty("gotItButton", true)
  }

  override fun isBold() = true
  override fun textColor() = JBUI.CurrentTheme.GotItTooltip.buttonForeground()
}

class SecondaryButtonZhabaAction : ZhabaCustomComponentAction() {
  override fun createButton(): JButton = ActionLink().apply {
    isFocusable = false
    isOpaque = false
    foreground = JBUI.CurrentTheme.GotItTooltip.secondaryActionForeground(false)
  }

  override fun isBold(): Boolean = false

  override fun textColor(): Color = JBUI.CurrentTheme.GotItTooltip.secondaryActionForeground(false)
}

abstract class ZhabaCustomComponentAction : CustomComponentAction {

  abstract fun createButton(): JButton
  abstract fun isBold(): Boolean
  abstract fun textColor(): Color

  override fun createCustomComponent(presentation: Presentation, place: String): JButton {
    val button = createButton()

    button.addActionListener { e ->
      val action = presentation.getClientProperty(CustomComponentAction.ACTION_KEY) ?: return@addActionListener

      val event = AnActionEvent.createEvent(
        action,
        DataManager.getInstance().getDataContext(e.source as JComponent),
        presentation,
        place,
        ActionUiKind.TOOLBAR,
        null
      )

      ActionUtil.performAction(action, event)
    }

    updateCustomComponent(button, presentation)
    return button
  }

  override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
    UIUtil.setEnabledRecursively(component, presentation.isEnabled)
    component.isVisible = presentation.isVisible

    val button = component as? JButton ?: return

    button.text = HtmlChunk.raw(presentation.text)
      .let { if (isBold()) it.bold() else it }
      .wrapWith(HtmlChunk.font(ColorUtil.toHtmlColor(textColor())))
      .wrapWith(HtmlChunk.html())
      .toString()
  }
}