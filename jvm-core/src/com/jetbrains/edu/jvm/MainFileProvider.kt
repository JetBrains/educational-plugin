package com.jetbrains.edu.jvm

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface MainFileProvider {
  fun findMainClass(project: Project, file: VirtualFile): String?

  companion object {
    @JvmField
    val EP_NAME = LanguageExtension<MainFileProvider>("Educational.mainFileProvider")

    fun getMainClassName(project: Project, file: VirtualFile, language: Language): String? {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.findMainClass(project, file)
    }
  }
}
