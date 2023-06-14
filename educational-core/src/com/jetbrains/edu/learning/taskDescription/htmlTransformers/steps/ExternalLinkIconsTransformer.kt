package com.jetbrains.edu.learning.taskDescription.htmlTransformers.steps

import com.jetbrains.edu.learning.taskDescription.addExternalLinkIcons
import com.jetbrains.edu.learning.taskDescription.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document

object ExternalLinkIconsTransformer : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    return addExternalLinkIcons(html)
  }
}