package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSeparator

class TopPanel(linkText: String, action: DumbAwareAction) : JPanel(BorderLayout()) {

  val actionLink = LightColoredActionLink(linkText, action, AllIcons.Actions.Back)

  init {
    actionLink.border = JBUI.Borders.emptyBottom(8)
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(15, 0, 8, 15)
    add(actionLink, BorderLayout.NORTH)
    add(JSeparator(), BorderLayout.SOUTH)
    maximumSize = JBUI.size(Int.MAX_VALUE, 30)
  }
}
