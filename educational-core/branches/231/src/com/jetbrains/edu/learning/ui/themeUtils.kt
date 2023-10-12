package com.jetbrains.edu.learning.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.IdeaActionButtonLook
import com.intellij.openapi.actionSystem.impl.Win10ActionButtonLook
import com.intellij.util.ui.UIUtil
import java.awt.Graphics
import javax.swing.JComponent

fun getCurrentThemeName(): String? {
  val lookAndFeel = LafManager.getInstance().currentLookAndFeel as? UIThemeBasedLookAndFeelInfo
  return lookAndFeel?.name
}

// BACKCOMPAT: 2023.2. Inline it
fun getCurrentThemeId(): String? {
  val lookAndFeel = LafManager.getInstance().currentLookAndFeel as? UIThemeBasedLookAndFeelInfo
  return lookAndFeel?.theme?.id
}

// BACKCOMPAT: 2023.1. Remove it
fun ActionButton.setLook() {
  setLook(ActionButtonLookWithHover())
}

private class ActionButtonLookWithHover : ActionButtonLook() {
  private var delegate: ActionButtonLook = if (UIUtil.isUnderWin10LookAndFeel()) Win10ActionButtonLook() else IdeaActionButtonLook()

  override fun paintBackground(g: Graphics?, component: JComponent?, state: Int) {
    delegate.paintBackground(g, component, state)
  }
}