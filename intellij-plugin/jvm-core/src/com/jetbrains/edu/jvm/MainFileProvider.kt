package com.jetbrains.edu.jvm

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

interface MainFileProvider {
  fun findMainClassName(project: Project, file: VirtualFile): String?

  fun findMainPsi(project: Project, file: VirtualFile): PsiElement?

  companion object {
    private val EP_NAME = LanguageExtension<MainFileProvider>("Educational.mainFileProvider")

    fun getMainClassName(project: Project, file: VirtualFile, language: Language): String? {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.findMainClassName(project, file)
    }

    fun getMainClass(project: Project, file: VirtualFile, language: Language): PsiElement? {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.findMainPsi(project, file)
    }
  }
}
