package com.jetbrains.edu.coursecreator.testGeneration.psi.manager

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile
import com.jetbrains.edu.coursecreator.testGeneration.processing.manager.TestPresenterManager
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiHelper

interface PsiHelperManager {

  fun getPsiHelper(psiFile: PsiFile): PsiHelper

  companion object {
    private val EP_NAME = LanguageExtension<PsiHelperManager>("Educational.PsiHelperManager")

    fun getInstance(language: Language): PsiHelperManager = EP_NAME.forLanguage(language)
  }
}
