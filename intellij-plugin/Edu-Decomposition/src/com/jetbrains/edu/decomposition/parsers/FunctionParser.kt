package com.jetbrains.edu.decomposition.parsers

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile


interface FunctionParser {
  fun extractFunctionNames(files: List<PsiFile>): List<String>

  companion object {
    private val EP_NAME = LanguageExtension<FunctionParser>("Educational.functionParser")

    fun extractFunctionNames(files: List<TaskFile>, project: Project, language: Language): List<String> {
      ThreadingAssertions.assertReadAccess()
      val psiFiles = files.mapNotNull { it.getVirtualFile(project) }.mapNotNull { PsiManager.getInstance(project).findFile(it) }
      return EP_NAME.forLanguage(language)?.extractFunctionNames(psiFiles) ?: emptyList()
    }
  }
}