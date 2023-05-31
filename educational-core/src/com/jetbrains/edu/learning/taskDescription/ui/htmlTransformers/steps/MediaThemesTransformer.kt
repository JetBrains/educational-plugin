package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps

import com.jetbrains.edu.learning.taskDescription.replaceMediaForTheme
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document

object MediaThemesTransformer : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task ?: return html
    return replaceMediaForTheme(context.project, task, html)
  }
}