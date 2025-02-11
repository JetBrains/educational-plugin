package com.jetbrains.edu.cognifiredecompose

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions

interface CognifireDecomposeDslPackageCallChecker {
  fun isCallFromCognifireDecomposeDslPackage(element: PsiElement): Boolean

  companion object {
    private val EP_NAME = LanguageExtension<CognifireDecomposeDslPackageCallChecker>("Educational.cognifireDecomposeDslPackageCallChecker")

    fun isCallFromCognifireDecomposeDslPackage(element: PsiElement, language: Language): Boolean {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.isCallFromCognifireDecomposeDslPackage(element)
        ?: error("Not supported to provide a `function` expression for the ${language.displayName} language")
    }
  }
}