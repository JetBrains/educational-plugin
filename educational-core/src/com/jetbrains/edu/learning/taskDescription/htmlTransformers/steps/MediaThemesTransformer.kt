package com.jetbrains.edu.learning.taskDescription.htmlTransformers.steps

import com.jetbrains.edu.learning.taskDescription.replaceMediaForTheme
import com.jetbrains.edu.learning.taskDescription.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document

object MediaThemesTransformer : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task
    return replaceMediaForTheme(context.project, task, html)
  }
}