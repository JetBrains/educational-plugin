package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext

object NoOperationTransformer : UIModeIndependentHtmlTransformer() {
  override fun transform(html: String, context: HtmlTransformerContext): String = html
}