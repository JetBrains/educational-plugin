package com.jetbrains.edu.codeInsight

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar

abstract class EduReferenceContributorBase : PsiReferenceContributor() {
  protected fun PsiReferenceRegistrar.registerEduReferenceProvider(provider: EduPsiReferenceProvider) {
    registerReferenceProvider(provider.pattern, provider)
  }
}

abstract class EduPsiReferenceProvider : PsiReferenceProvider() {
  abstract val pattern: ElementPattern<out PsiElement>
}
