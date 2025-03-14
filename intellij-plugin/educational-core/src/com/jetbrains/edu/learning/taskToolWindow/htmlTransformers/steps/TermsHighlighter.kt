package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.taskToolWindow.*
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TheoryLookupSettings
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Highlights terms in an HTML document by adding dashed underline style to the occurrences of the terms.
 * Takes terms from the [TermsProjectSettings] object.
 *
 * @see HtmlTransformer
 * @see TermsProjectSettings
 */
object TermsHighlighter : HtmlTransformer {
  // TODO: filter code blocks and other tags
  private val INVALID_TAGS = setOf(CODE_TAG, A_TAG, IMG_TAG)

  private fun Element.isValidTag(): Boolean = tagName() !in INVALID_TAGS

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    if (!TheoryLookupSettings.getInstance().isTheoryLookupEnabled || context.task !is TheoryTask) return html
    val task = context.task
    val language = TranslationProjectSettings.getInstance(context.project).translationLanguage
    if (language != null && (language.code != TranslationLanguage.ENGLISH.code || language.code != task.course.languageCode)) return html
    val terms = TermsProjectSettings.getInstance(context.project).getTaskTerms(task)?.map { it.value }
    if (terms.isNullOrEmpty()) return html
    for (termTitle in terms.sortedBy { it.length }.reversed()) {
      formatTermOccurrences(html, termTitle)
    }
    return html
  }

  private fun formatTermOccurrences(html: Document, termTitle: String) {
    val nodes = html.getElementsContainingOwnText(termTitle)
      .asSequence()
      .filter { it.isValidTag() }
      .flatMap { it.textNodes() }
      .filter { it.text().contains(termTitle) }
    for (node in nodes) {
      val termElement = getDashedUnderlineElement(html, termTitle)

      val text = node.wholeText
      val startIdx = text.indexOf(termTitle)
      // if there is no occurrence in the node -> continue
      if (startIdx == -1) continue

      // if the char before the occurrence is a letter -> continue
      if (startIdx > 0 && text[startIdx - 1].isLetter()) continue

      val endIdx = startIdx + termTitle.length
      val tail = if (endIdx < text.length) node.splitText(endIdx) else null

      // if the char after the occurrence is a letter -> continue
      if (tail?.wholeText?.firstOrNull()?.isLetter() == true) continue

      // then we found the occurrence
      val middle = node.splitText(startIdx)
      middle.after(termElement.clone())
      middle.remove()
      return
    }
  }
}
