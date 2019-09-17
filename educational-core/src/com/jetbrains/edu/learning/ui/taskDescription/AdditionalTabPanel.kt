package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckDetailsPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextPane
import javax.swing.event.HyperlinkListener

class AdditionalTabPanel(project: Project) : JPanel() {

  private val textPane: JTextPane = createTextPane()

  init {
    val scrollPane = JBScrollPane(textPane)
    scrollPane.border = JBUI.Borders.empty()
    val backLinkPanel = getBackLinkPanel(project)

    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(8, 16, 0, 0)

    add(backLinkPanel, Component.LEFT_ALIGNMENT)
    add(scrollPane, Component.LEFT_ALIGNMENT)
  }

  fun setText(text: String) {
    textPane.text = text
  }

  fun addHyperlinkListener(listener: HyperlinkListener) {
    textPane.addHyperlinkListener(listener)
  }

  private fun getBackLinkPanel(project: Project): JPanel {
    val backLink = LightColoredActionLink(
      "Back to the task description",
      CheckDetailsPanel.SwitchTaskTabAction(project, 0),
      AllIcons.Actions.Back)
    backLink.border = JBUI.Borders.emptyBottom(8)
    val backLinkPanel = JPanel(BorderLayout())
    backLinkPanel.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    backLinkPanel.border = JBUI.Borders.empty(0, 0, 8, 15)
    backLinkPanel.add(backLink, BorderLayout.NORTH)
    backLinkPanel.add(JSeparator(), BorderLayout.SOUTH)
    backLinkPanel.maximumSize = JBUI.size(Int.MAX_VALUE, 30)
    return backLinkPanel
  }
}