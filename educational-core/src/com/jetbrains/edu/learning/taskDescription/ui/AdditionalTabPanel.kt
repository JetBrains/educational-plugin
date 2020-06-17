package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckDetailsPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextPane
import javax.swing.event.HyperlinkListener

open class AdditionalTabPanel(val project: Project, tabName: String) : JPanel(BorderLayout()) {

  protected val textPane: JTextPane = createTextPane()

  init {
    name = tabName
    val scrollPane = JBScrollPane(textPane)
    scrollPane.border = JBUI.Borders.empty()
    val backLinkPanel = getBackLinkPanel()

    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(8, 16, 0, 0)

    addComponent(backLinkPanel, BorderLayout.BEFORE_FIRST_LINE)
    addComponent(scrollPane, BorderLayout.CENTER)
  }

  fun setText(text: String) {
    textPane.text = text
  }

  fun addHyperlinkListener(listener: HyperlinkListener) {
    textPane.addHyperlinkListener(listener)
  }

  private fun addComponent(comp: Component, constraints: String) {
    add(comp, constraints)
  }

  private fun getBackLinkPanel(): JPanel {
    val backLink = LightColoredActionLink(
      EduCoreBundle.message("label.back.to.description"),
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