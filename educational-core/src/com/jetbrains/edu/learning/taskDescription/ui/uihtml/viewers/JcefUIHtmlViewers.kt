package com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.JcefHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.NoOperationTransformer

class JcefUIHtmlViewer(project: Project, private val htmlTransformer: JcefHtmlTransformer = NoOperationTransformer) : UIHtmlViewer() {

  val htmlPane = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)
  override val component = htmlPane.component

  var context: HtmlTransformerContext? = null
    private set

  override fun setHtmlWithContext(html: String, context: HtmlTransformerContext) {
    this.context = context
    val processedHtml = htmlTransformer.jcefTransform(html, context)
    htmlPane.loadHTML(processedHtml)
  }

  init {
    Disposer.register(this, htmlPane)
    htmlTransformer.setupJcefPanel(project, this)
  }
}