package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.io.URLUtil
import javax.swing.Icon

abstract class EduUriPathCompletionContributorBase : CompletionProvider<CompletionParameters>() {

  protected abstract val elementTextPrefix: String

  protected abstract fun collectPathCompletionVariants(parameters: CompletionParameters): List<UriPathCompletionVariant>

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val position = parameters.position

    val currentText = position.text.substring(0, parameters.offset - position.textRange.startOffset)
    if (!currentText.startsWith(elementTextPrefix)) return

    val variants = collectPathCompletionVariants(parameters)

    val lookupElements = variants.map { (id, presentableText, icon) ->
      LookupElementBuilder.create(URLUtil.encodePath(id))
        .withPresentableText(presentableText)
        .withIcon(icon)
    }

    result.withPrefixMatcher(currentText.substringAfter(elementTextPrefix))
      .addAllElements(lookupElements)
  }

  protected data class UriPathCompletionVariant(
    val path: String,
    val presentableText: String,
    val icon: Icon?
  )
}
