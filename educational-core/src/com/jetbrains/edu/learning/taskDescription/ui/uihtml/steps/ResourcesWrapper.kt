package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.jetbrains.edu.learning.taskDescription.ui.htmlWithResources
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer

object ResourceWrapper : UIModeIndependentHtmlTransformer() {
  override fun transform(html: String, context: HtmlTransformerContext): String = htmlWithResources(context.project, html, context.task)
}