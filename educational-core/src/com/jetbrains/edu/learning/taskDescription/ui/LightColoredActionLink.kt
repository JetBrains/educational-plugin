package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.JBColor
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import javax.swing.Icon


class LightColoredActionLink(text: String, action: AnAction, icon: Icon? = null) : ActionLink(text, icon, action) {
  init {
    setNormalColor(JBColor(0x6894C6, 0x5C84C9))
    border = JBUI.Borders.empty(16, 0, 0, 16)
  }
}