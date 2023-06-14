package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.NlsContexts.LinkLabel
import com.intellij.ui.components.AnActionLink
import com.intellij.util.ui.JBUI
import javax.swing.Icon
import javax.swing.SwingConstants

class LightColoredActionLink(@LinkLabel text: String,
                             action: AnAction,
                             actionIcon: Icon? = null,
                             isExternal: Boolean = false
) : AnActionLink(text, action) {

  init {
    if (isExternal) {
      icon = AllIcons.Ide.External_link_arrow
      iconTextGap = 0
      horizontalTextPosition = SwingConstants.LEFT
    }
    else {
      icon = actionIcon
    }
    border = JBUI.Borders.empty(16, 0, 0, 16)
  }
}