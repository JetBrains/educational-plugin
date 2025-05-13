package com.jetbrains.edu.ai.debugger.core.slicing

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface SliceManager {

  /**
   * Processes a given element to compute a set of slice-related line numbers for intermediate breakpoints.
   *
   * @param element The PSI element to start analysing slice processing.
   * @param document The document corresponding to the PSI file in which slicing is performed.
   * @param psiFile The file containing the PSI element being processed.
   * @return A set of integers representing line numbers on which intermediate breakpoints can be placed based on slicing.
   */
  fun processSlice(element: PsiElement, document: Document, psiFile: PsiFile): Set<Int>

  companion object {
    private val EP_NAME = LanguageExtension<SliceManager>("Educational.sliceManager")

    fun getInstance(language: Language): SliceManager = EP_NAME.forLanguage(language)
  }
}
