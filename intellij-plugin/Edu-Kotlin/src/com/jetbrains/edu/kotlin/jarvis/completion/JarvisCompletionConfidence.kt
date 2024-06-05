package com.jetbrains.edu.kotlin.jarvis.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.kotlin.jarvis.DescriptionRunLineMarkerContributor.Companion.DESCRIPTION
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry

class JarvisCompletionConfidence : CompletionConfidence() {
  override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
    val functionElement = contextElement.parent.parent.parent.parent.parent
    if (contextElement.parent is KtLiteralStringTemplateEntry &&
        JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(functionElement, functionElement.language) &&
        functionElement.text.startsWith(DESCRIPTION)) {
      return ThreeState.NO
    }

    return ThreeState.UNSURE
  }
}
