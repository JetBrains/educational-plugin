package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.util.ProcessingContext
import com.intellij.util.io.URLUtil
import javax.swing.Icon

abstract class EduUriPathCompletionContributorBase : CompletionProvider<CompletionParameters>() {

  protected abstract val elementTextPrefix: String

  protected abstract fun collectUriPathLookupElements(parameters: CompletionParameters): List<UriPathLookupElement>

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val position = parameters.position

    val currentText = position.text.substring(0, parameters.offset - position.textRange.startOffset)
    if (!currentText.startsWith(elementTextPrefix)) return

    val elements = collectUriPathLookupElements(parameters)

    result.withPrefixMatcher(UriPathPrefixMatcher(currentText.substringAfter(elementTextPrefix)))
      .addAllElements(elements)
  }

  protected class UriPathLookupElement(
    private val path: String,
    private val displayName: String,
    private val icon: Icon?
  ) : LookupElement() {

    val displayNameLookupElement: LookupElement by lazy {
      LookupElementBuilder.create(displayName)
        .withCaseSensitivity(isCaseSensitive)
    }

    override fun getLookupString(): String = URLUtil.encodePath(path)
    override fun isCaseSensitive(): Boolean = false
    override fun renderElement(presentation: LookupElementPresentation) {
      presentation.icon = icon
      presentation.itemText = if (displayName.equals(path, true)) displayName else "$displayName ($path)"
    }
  }

  private class UriPathPrefixMatcher(prefix: String) : CamelHumpMatcher(prefix) {

    override fun prefixMatches(element: LookupElement): Boolean {
      if (super.prefixMatches(element)) return true
      // Properly process lookup elements wrapped into `com.intellij.codeInsight.lookup.LookupElementDecorator`
      val baseElement = element.`as`(UriPathLookupElement::class.java) ?: return false
      // We expect that users most likely type display names instead of real paths (IDs)
      // because actual IDs are hidden knowledge, but display names are shown in IDE UI.
      // So let's select elements if their display name matches prefix even if it completely differs with actual ID
      //
      // A separate lookup element is needed to invoke proper overload of `prefixMatches` method.
      // Otherwise, case sensitivity won't be checked properly in some cases
      return super.prefixMatches(baseElement.displayNameLookupElement)
    }

    override fun cloneWithPrefix(prefix: String): PrefixMatcher {
      return if (prefix == myPrefix) this else UriPathPrefixMatcher(prefix)
    }

    override fun isCaseSensitive(): Boolean = false
  }
}
