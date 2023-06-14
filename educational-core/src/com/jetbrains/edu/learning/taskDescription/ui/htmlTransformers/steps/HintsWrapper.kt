package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps

import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
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
        JavaUILibrary.JCEF -> wrapHintJCEF(context.project, e, number, title)
        JavaUILibrary.SWING -> wrapHintSwing(context.project, e, number, title)
        // all other options are deprecated, but we anyway should process them:
        else -> e.html()
      }
    }
  }
}