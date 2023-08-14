package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.NodeFilter.FilterResult.CONTINUE
import org.jsoup.select.NodeFilter.FilterResult.REMOVE
import javax.swing.JTextPane

/**
 * Remote all `style` tags from task description if Swing is used as rendering engine
 * because `style` tags are not processed by [JTextPane]
 */
object CssHtmlTransformer : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    if (context.uiMode != JavaUILibrary.SWING) return html

    html.filter { node, _ ->
      if (node is Element && node.tagName() == "style") REMOVE else CONTINUE
    }

    return html
  }
}
