package com.jetbrains.edu.cognifire.inspection

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

interface InspectionProcessor {
  fun applyInspections(promptToCodeTranslation: PromptToCodeResponse, project: Project, psiFile: PsiFile): PromptToCodeResponse

  companion object {
    private val EP_NAME = LanguageExtension<InspectionProcessor>("Educational.inspectionProcessor")

    private fun applyInspections(promptToCodeTranslation: PromptToCodeResponse, project: Project, psiFile: PsiFile, language: Language): PromptToCodeResponse? {
      return EP_NAME.forLanguage(language)?.applyInspections(promptToCodeTranslation, project, psiFile)
    }

    fun applyInspections(promptToCodeTranslation: PromptToCodeResponse, functionSignature: String, project: Project, language: Language): PromptToCodeResponse? {
      var code = promptToCodeTranslation.distinctBy { it.codeLineNumber }.joinToString(System.lineSeparator()) { it.generatedCodeLine }
      code = """
        $functionSignature {
            $code
        }
      """.trimIndent()
      val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("Main.kt", language, code) }
      return applyInspections(promptToCodeTranslation, project, psiFile, language)
    }
  }
}
