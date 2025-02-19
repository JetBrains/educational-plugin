package com.jetbrains.edu.aiDebugging.core.slicing

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface SliceManager {

  fun processSlice(element: PsiElement, document: Document, psiFile: PsiFile): Set<Int>

  companion object {
    private val EP_NAME = LanguageExtension<SliceManager>("aiDebugging.sliceManager")

    @Suppress("unused")
    fun getInstance(language: Language): SliceManager = EP_NAME.forLanguage(language)
  }
}
