package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.event.HyperlinkListener

open class AdditionalTabPanel(val project: Project, tabName: String) : JPanel(BorderLayout()) {

  protected val textPane: JTextPane = createTextPane()

  init {
    name = tabName
    val scrollPane = JBScrollPane(textPane)
    scrollPane.border = JBUI.Borders.empty()

    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(15, 15, 0, 0)

    this.add(scrollPane, BorderLayout.CENTER)
  }

  fun setText(text: String) {
    textPane.text = text
  }

  fun addHyperlinkListener(listener: HyperlinkListener) {
    textPane.addHyperlinkListener(listener)
  }
}
