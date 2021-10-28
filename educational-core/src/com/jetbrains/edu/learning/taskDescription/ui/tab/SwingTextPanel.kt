package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.SwingToolWindow
import com.jetbrains.edu.learning.taskDescription.ui.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.createTextPane
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import org.jsoup.nodes.Element
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane


class SwingTextPanel(project: Project, linkHandler: SwingToolWindowLinkHandler? = null) : TabTextPanel(project) {
  private val textPane: JTextPane = createTextPane()
  private var currentLinkHandler: SwingToolWindowLinkHandler? = null

  override val component: JComponent = textPane

  init {
    val scrollPane = JBScrollPane(textPane)
    scrollPane.border = JBUI.Borders.empty()
    add(scrollPane, BorderLayout.CENTER)
    updateLinkHandler(linkHandler)
  }

  override fun setText(text: String) {
    textPane.text = text
  }

  override fun wrapHint(hintElement: Element, displayedHintNumber: String): String {
    return SwingToolWindow.wrapHint(project, hintElement, displayedHintNumber)
  }

  fun updateLinkHandler(newHyperlinkListener: SwingToolWindowLinkHandler?) {
    textPane.removeHyperlinkListener(currentLinkHandler)
    val linkHandler = newHyperlinkListener ?: SwingToolWindowLinkHandler(project)
    textPane.addHyperlinkListener(linkHandler)
    currentLinkHandler = linkHandler
  }

  fun addLoadingSubmissionsPanel(platformName: String) {
    removeAll()
    val asyncProcessIcon = AsyncProcessIcon("Loading submissions")
    val iconPanel = JPanel(FlowLayout(FlowLayout.LEADING))
    iconPanel.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    iconPanel.add(asyncProcessIcon)
    setTabText("<a ${StyleManager().textStyleHeader}>${EduCoreBundle.message("submissions.loading", platformName)}")
    iconPanel.add(textPane)
    add(iconPanel, BorderLayout.CENTER)
    revalidate()
  }
}
