package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.StringHtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.ui.htmlWithResources

object ResourceWrapper : StringHtmlTransformer {
  override fun transform(html: String, context: HtmlTransformerContext): String = htmlWithResources(context.project, html, context.task)
}