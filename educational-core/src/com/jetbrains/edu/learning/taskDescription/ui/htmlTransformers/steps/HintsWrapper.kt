package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps

import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlUIMode
import com.jetbrains.edu.learning.taskDescription.ui.wrapHintJCEF
import com.jetbrains.edu.learning.taskDescription.ui.wrapHintSwing
import com.jetbrains.edu.learning.taskDescription.ui.wrapHintTagsInsideHTML
import org.jsoup.nodes.Document

object HintsWrapper : HtmlTransformer {

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    if (context.task is TheoryTask)
      return html

    return wrapHintTagsInsideHTML(html) { e, number, title ->
      return@wrapHintTagsInsideHTML when (context.uiMode) {
        HtmlUIMode.JCEF -> wrapHintJCEF(context.project, e, number, title)
        HtmlUIMode.SWING -> wrapHintSwing(context.project, e, number, title)
      }
    }
  }
}