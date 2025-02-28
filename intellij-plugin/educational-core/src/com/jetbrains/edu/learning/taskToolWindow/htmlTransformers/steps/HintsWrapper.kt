package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskToolWindow.ui.wrapHintJCEF
import com.jetbrains.edu.learning.taskToolWindow.ui.wrapHintSwing
import com.jetbrains.edu.learning.taskToolWindow.ui.wrapHintTagsInsideHTML
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

object HintsWrapper : HtmlTransformer {

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    if (context.task.course is HyperskillCourse) {
      return html
    }

    return wrapHintTagsInsideHTML(html) { e, number, title ->
      e.fixFirstParagraph()

      when (context.uiMode) {
        JavaUILibrary.JCEF -> wrapHintJCEF(context.project, e, number, title)
        JavaUILibrary.SWING -> wrapHintSwing(context.project, e, number, title)
        // all other options are deprecated, but we anyway should process them:
        else -> e.html()
      }
    }
  }

  /**
   * Marks the first paragraph of the hint with the `first-paragraph` class.
   * But only if there is no text before it. Only some blank symbols are allowed before the first paragraph.
   * The class is needed because the condition that there is no text before the paragraph is impossible to be tested with CSS.
   *
   * # Why this
   *
   * Many course authors write hints as
   * ```
   * <div class='hint'>hint **text**</div>
   * ```
   * or
   * ```
   * <div class='hint'>
   *   hint **text**
   *
   *   another paragraph
   * </div>
   * ```
   *
   * In this case, the `hint **text**` is treated by the Markdown parser as plain HTML because it is a continuation of the HTML block with
   * the <div> tag.
   * And the Markdown interpreter outputs the following result:
   * ```
   * <div class='hint'>
   *   hint **text**
   *
   *   <p>another paragraph</p>
   * </div>
   * ```
   *
   * The fix, according to the [Markdown spec](https://github.github.com/gfm/#html-blocks), is to make a blank line after the `<div>`:
   *
   * ```
   * <div class='hint'>
   *
   *   hint **text**
   *
   *   another paragraph
   * </div>
   * ```
   *
   * In this case the interpreter outputs:
   * ```
   * <div class='hint'>
   *   <p>hint <strong>text</strong></p>
   *
   *   <p>another paragraph</p>
   * </div>
   * ```
   *
   * We need to make sure that the design of the raw text in the beginning of `<div>` is the same as if this text is wrapped with `<p>`.
   * So we add a class to the first paragraph.
   */
  private fun Element.fixFirstParagraph() {
    for (childNode in childNodes()) {
      // skip comments and blank texts
      if (childNode is Comment) continue
      val isBlankText = childNode is TextNode && childNode.text().trim().isEmpty()
      if (isBlankText) continue

      // if the first non-comment or blank node is a paragraph, mark it
      if (childNode is Element && childNode.tagName() == "p") {
        childNode.addClass("first-paragraph")
      }

      return
    }
  }
}