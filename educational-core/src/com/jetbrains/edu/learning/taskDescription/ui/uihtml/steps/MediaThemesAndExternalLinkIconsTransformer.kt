package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.jetbrains.edu.learning.taskDescription.addExternalLinkIcons
import com.jetbrains.edu.learning.taskDescription.replaceMediaForTheme
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer

object MediaThemesAndExternalLinkIconsTransformer : UIModeIndependentHtmlTransformer() {
  override fun transform(html: String, context: HtmlTransformerContext): String {
    val task = context.task ?: return html
    val mediaReplaced = replaceMediaForTheme(context.project, task, html)
    return addExternalLinkIcons(mediaReplaced)
  }
}