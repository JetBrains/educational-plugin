package com.jetbrains.edu.learning.eduAssistant.context.differ

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.TaskFile

interface FilesDiffer {
  fun findChangedMethods(before: PsiFile, after: PsiFile, taskFile: TaskFile): String

  companion object {
    private val EP_NAME = LanguageExtension<FilesDiffer>("Educational.filesDiffer")

    fun findDifferentMethods(before: PsiFile, after: PsiFile, taskFile: TaskFile, language: Language): String? {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.findChangedMethods(before, after, taskFile)
    }
  }
}