package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * This enum is a part of the context used to transform HTML.
 * It specifies the UI component that will display the resulting HTML.
 * Different components support HTML differently, so during transformation, it is important to use only those HTML elements and CSS styles
 * that are supported by the component.
 *
 * The [HtmlUIMode] specifies not only the type of the component to display the resulting element, but also the way these components
 * are set up.
 * Among other things, the component should have listeners to properly handle HTML links inside the generated HTML.
 */
enum class HtmlUIMode {
  /**
   * [JCEF] UI mode means that the resulting HTML will be displayed in a properly set up [JCEFHtmlPanel].
   */
  JCEF,

  /**
   * [SWING] UI mode means that the resulting HTML will be displayed inside a properly set up [javax.swing.JTextPane].
   */
  SWING
}

/**
 * This is the data needed during html transformation.
 */
data class HtmlTransformerContext(val project: Project, val task: Task?, val uiMode: HtmlUIMode)

/**
 * This is a transformer that takes one HTML and transforms it to another HTML.
 * The `context` argument influences the way the html is processed.
 *
 * This transformer works with a parsed HTML document, it may modify the passed document.
 */
interface HtmlTransformer {
  fun transform(html: Document, context: HtmlTransformerContext): Document

  fun toStringTransformer() = object : StringHtmlTransformer {
    override fun transform(html: String, context: HtmlTransformerContext): String {
      val document = Jsoup.parse(html)
      val transformedHtml = this@HtmlTransformer.transform(document, context)
      return transformedHtml.toString()
    }
  }
}

/**
 * This is a class similar to [HtmlTransformer], but it works with HTML in the string form.
 */
interface StringHtmlTransformer {
  fun transform(html: String, context: HtmlTransformerContext): String
}

fun pipeline(vararg transformers: StringHtmlTransformer): StringHtmlTransformer = object : StringHtmlTransformer {
  override fun transform(html: String, context: HtmlTransformerContext): String {
    var result = html
    for (transformer in transformers) {
      result = transformer.transform(result, context)
    }
    return result
  }
}

fun pipeline(vararg transformers: HtmlTransformer): HtmlTransformer = object : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    var result = html
    for (transformer in transformers) {
      result = transformer.transform(result, context)
    }
    return result
  }
}