package com.jetbrains.edu.kotlin.cognifire.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import com.jetbrains.edu.kotlin.cognifire.psi.ElementSearch
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry


/**
 * Decides whether the autocomplete functionality should be skipped or not within a given context for the Cognifire application.
 * If the current element is part of a string literal in a `prompt` method call, the autocomplete functionality shouldn't be skipped.
 *
 * @see CompletionConfidence
 */
class CognifireCompletionConfidence : CompletionConfidence() {
  override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
    val promptBlock = ElementSearch.findPromptElement(contextElement) { it.parent }
    if (promptBlock != null && contextElement.parent is KtLiteralStringTemplateEntry) {
      return ThreeState.NO
    }

    return ThreeState.UNSURE
  }
}