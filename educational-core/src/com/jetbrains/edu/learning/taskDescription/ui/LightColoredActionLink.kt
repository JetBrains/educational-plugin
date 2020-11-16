package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.ui.EduColors
import javax.swing.Icon
import javax.swing.SwingConstants

class LightColoredActionLink(text: String,
                             action: AnAction,
                             actionIcon: Icon? = null,
                             isExternal: Boolean = false
) : ActionLink(text, action) {

  init {
    if (isExternal) {
      icon = AllIcons.Ide.External_link_arrow
      iconTextGap = 0
      horizontalTextPosition = SwingConstants.LEFT
    }
    else {
      icon = actionIcon
    }
    setNormalColor(EduColors.hyperlinkColor)
    border = JBUI.Borders.empty(16, 0, 0, 16)
  }
}