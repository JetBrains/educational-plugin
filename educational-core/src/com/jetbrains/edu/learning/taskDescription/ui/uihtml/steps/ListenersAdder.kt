package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.handlers.HintElementLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.handlers.JcefTaskInfoLifeSpanHandler
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.handlers.JcefToolWindowRequestHandler
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.handlers.JcefToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.JcefUIHtmlViewer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.SwingUIHtmlViewer

object ListenersAdder : HtmlTransformer {
  override fun swingTransform(html: String, context: HtmlTransformerContext): String = html

  override fun setupSwingPanel(project: Project, htmlViewer: SwingUIHtmlViewer) {
    val textPane = htmlViewer.textPane
    val toolWindowLinkHandler = HintElementLinkHandler(project, textPane)
    textPane.addHyperlinkListener(toolWindowLinkHandler)
  }

  override fun jcefTransform(html: String, context: HtmlTransformerContext): String = html

  override fun setupJcefPanel(project: Project, htmlViewer: JcefUIHtmlViewer) {
    val htmlPane = htmlViewer.htmlPane
    val jcefLinkInToolWindowHandler = JcefToolWindowLinkHandler(project)
    val taskInfoRequestHandler = JcefToolWindowRequestHandler(jcefLinkInToolWindowHandler)
    htmlPane.jbCefClient.addRequestHandler(taskInfoRequestHandler, htmlPane.cefBrowser)
    val taskInfoLifeSpanHandler = JcefTaskInfoLifeSpanHandler(jcefLinkInToolWindowHandler)
    htmlPane.jbCefClient.addLifeSpanHandler(taskInfoLifeSpanHandler, htmlPane.cefBrowser)
  }
}

