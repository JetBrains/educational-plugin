package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.ui.EduColors
import javax.swing.Icon


class LightColoredActionLink(text: String, action: AnAction, icon: Icon? = null) : ActionLink(text, icon, action) {
  init {
    setNormalColor(EduColors.hyperlinkColor)
    border = JBUI.Borders.empty(16, 0, 0, 16)
  }
}