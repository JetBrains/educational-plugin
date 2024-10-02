package com.jetbrains.edu.cognifire.inspection

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.util.concurrency.ThreadingAssertions

interface InspectionProcessor {
  fun applyInspections(project: Project, psiFile: PsiFile)

  companion object {
    private val EP_NAME = LanguageExtension<InspectionProcessor>("Educational.inspectionProcessor")

    private fun applyInspections(project: Project, psiFile: PsiFile, language: Language) {
      ThreadingAssertions.assertReadAccess()
      EP_NAME.forLanguage(language)?.applyInspections(project, psiFile)
    }

    fun applyInspections(code: String, project: Project, language: Language): String {
      val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("Main.kt", language, code) }
      runReadAction { applyInspections(project, psiFile, language) }
      return runReadAction<String> { psiFile.text }
    }
  }
}
