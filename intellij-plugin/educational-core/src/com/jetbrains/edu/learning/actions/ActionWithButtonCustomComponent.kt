package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.util.ui.UIUtil
import javax.swing.AbstractButton
import javax.swing.JButton
import javax.swing.JComponent

// BACKCOMPAT: 2024.2. Decouple custom component creation from `AnAction`.
// Since 2024.3 it's possible to have a separate `CustomComponentAction` object to provide a custom component for action
// instead of implementing it for the action itself.
// It may allow using different components in different cases with the same action object via passing different
// `CustomComponentAction` implementations using `com.intellij.openapi.actionSystem.ex.ActionUtil#COMPONENT_PROVIDER`
abstract class ActionWithButtonCustomComponent : AnAction(), CustomComponentAction {
  override fun createCustomComponent(
    presentation: Presentation,
    place: String
  ): JButton {
    val button = JButton(presentation.text)
    button.toolTipText = presentation.description
    button.addActionListener { e ->
      ActionUtil.invokeAction(this, button, place, null, null)
    }
    return button
  }

  override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
    UIUtil.setEnabledRecursively(component, presentation.isEnabled)
    component.isVisible = presentation.isVisible
    component.toolTipText = presentation.description
    if (component is AbstractButton) {
      component.text = presentation.text
    }
  }
}
