package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.taskDescription.ui.JCEFTaskInfoLifeSpanHandler
import com.jetbrains.edu.learning.taskDescription.ui.JCEFToolWindowRequestHandler
import com.jetbrains.edu.learning.taskDescription.ui.JCefToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.SwingToolWindow
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.JcefUIHtmlViewer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.SwingUIHtmlViewer

object ListenersAdder : HtmlTransformer {
  override fun swingTransform(html: String, context: HtmlTransformerContext): String = html

  override fun setupSwingPanel(project: Project, htmlViewer: SwingUIHtmlViewer) {
    val textPane = htmlViewer.textPane
    val toolWindowLinkHandler = SwingToolWindow.HintElementLinkHandler(project, textPane)
    textPane.addHyperlinkListener(toolWindowLinkHandler)
  }

  override fun jcefTransform(html: String, context: HtmlTransformerContext): String = html

  override fun setupJcefPanel(project: Project, htmlViewer: JcefUIHtmlViewer) {
    val htmlPane = htmlViewer.htmlPane
    val jcefLinkInToolWindowHandler = JCefToolWindowLinkHandler(project)
    val taskInfoRequestHandler = JCEFToolWindowRequestHandler(jcefLinkInToolWindowHandler)
    htmlPane.jbCefClient.addRequestHandler(taskInfoRequestHandler, htmlPane.cefBrowser)
    val taskInfoLifeSpanHandler = JCEFTaskInfoLifeSpanHandler(jcefLinkInToolWindowHandler)
    htmlPane.jbCefClient.addLifeSpanHandler(taskInfoLifeSpanHandler, htmlPane.cefBrowser)
  }
}

