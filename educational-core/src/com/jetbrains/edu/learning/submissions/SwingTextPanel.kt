package com.jetbrains.edu.learning.submissions

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.createTextPane
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.JTextPane

class SwingTextPanel(private val project: Project, linkHandler: SwingToolWindowLinkHandler? = null) : JPanel(BorderLayout()) {
  private var currentLinkHandler: SwingToolWindowLinkHandler? = null

  private val textPane: JTextPane = createTextPane()
  private val textPanel: JBScrollPane = JBScrollPane(textPane)

  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(15, 15, 0, 0)

    textPanel.border = JBUI.Borders.empty()
    add(textPanel, BorderLayout.CENTER)

    updateLinkHandler(linkHandler)
  }

  fun setText(text: String) {
    textPane.text = text
  }

  fun updateLinkHandler(newHyperlinkListener: SwingToolWindowLinkHandler?) {
    textPane.removeHyperlinkListener(currentLinkHandler)
    val linkHandler = newHyperlinkListener ?: SwingToolWindowLinkHandler(project)
    textPane.addHyperlinkListener(linkHandler)
    currentLinkHandler = linkHandler
  }

  fun showLoadingSubmissionsPanel(platformName: String) {
    removeAll()
    val loadingPanel = createLoadingPanel(platformName)
    add(loadingPanel)
    revalidate()
  }

  fun hideLoadingSubmissionsPanel() {
    removeAll()
    add(textPanel)
    revalidate()
  }

  private fun createLoadingPanel(platformName: String): JPanel {
    val loadingPanel = JPanel(FlowLayout(FlowLayout.LEADING))
    val asyncProcessIcon = AsyncProcessIcon("Loading submissions")
    val loadingTextPane: JTextPane = createTextPane().apply {
      @NonNls
      val styledText = "<a ${StyleManager().textStyleHeader}>${EduCoreBundle.message("submissions.loading", platformName)}"
      text = styledText
    }
    loadingPanel.apply {
      background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
      add(asyncProcessIcon)
      add(loadingTextPane)
    }
    return loadingPanel
  }
}
