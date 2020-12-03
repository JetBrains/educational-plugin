package com.jetbrains.edu.markdown.taskDescription

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceRegistrar
import com.jetbrains.edu.codeInsight.EduReferenceContributorBase
import com.jetbrains.edu.codeInsight.taskDescription.InCourseLinkReferenceProviderBase

class EduMarkdownReferenceContributor : EduReferenceContributorBase() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerEduReferenceProvider(MarkdownInCourseLinkReferenceProvider())
  }
}

private class MarkdownInCourseLinkReferenceProvider : InCourseLinkReferenceProviderBase() {
  override val pattern: ElementPattern<out PsiElement>
    get() = EduMarkdownPsiPatterns.markdownLinkDestination
  override val PsiElement.value: String?
    get() = text
}
