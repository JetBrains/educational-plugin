package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * This is the data needed during html transformation.
 */
data class HtmlTransformerContext(val project: Project, val task: Task, val uiMode: JavaUILibrary)

/**
 * This is a transformer that takes one HTML and transforms it to another HTML.
 * The `context` argument influences the way the html is processed.
 *
 * This transformer works with a parsed HTML document, it may modify the passed document.
 */
interface HtmlTransformer {
  fun transform(html: Document, context: HtmlTransformerContext): Document

  fun toStringTransformer(): StringHtmlTransformer = object : StringHtmlTransformer {
    override fun transform(html: String, context: HtmlTransformerContext): String {
      val document = Jsoup.parse(html)
      val transformedHtml = transform(document, context)
      return transformedHtml.toString()
    }
  }

  companion object {
    fun pipeline(vararg transformers: HtmlTransformer): HtmlTransformer = object : HtmlTransformer {
      override fun transform(html: Document, context: HtmlTransformerContext): Document {
        var result = html
        for (transformer in transformers) {
          result = transformer.transform(result, context)
        }
        return result
      }
    }
  }
}

/**
 * This is a class similar to [HtmlTransformer], but it works with HTML in the string form.
 */
interface StringHtmlTransformer {
  fun transform(html: String, context: HtmlTransformerContext): String

  companion object {
    fun pipeline(vararg transformers: StringHtmlTransformer): StringHtmlTransformer = object : StringHtmlTransformer {
      override fun transform(html: String, context: HtmlTransformerContext): String {
        var result = html
        for (transformer in transformers) {
          result = transformer.transform(result, context)
        }
        return result
      }
    }
  }
}