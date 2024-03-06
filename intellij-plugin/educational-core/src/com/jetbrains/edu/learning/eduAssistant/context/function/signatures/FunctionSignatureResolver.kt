package com.jetbrains.edu.learning.eduAssistant.context.function.signatures

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignature

interface FunctionSignatureResolver {

  fun getFunctionBySignature(psiFile: PsiFile, functionSignature: FunctionSignature): PsiElement?

  companion object {
    private val EP_NAME = LanguageExtension<FunctionSignatureResolver>("Educational.functionSignatureResolver")

    fun getFunctionBySignature(psiFile: PsiFile, functionSignature: FunctionSignature, language: Language): PsiElement? {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.getFunctionBySignature(psiFile, functionSignature)
    }
  }
}