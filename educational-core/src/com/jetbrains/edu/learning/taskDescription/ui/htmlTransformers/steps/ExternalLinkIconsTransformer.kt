package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps

import com.jetbrains.edu.learning.taskDescription.addExternalLinkIcons
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document

object ExternalLinkIconsTransformer : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    return addExternalLinkIcons(html)
  }
}