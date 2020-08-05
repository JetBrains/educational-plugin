package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface MainFileProvider {
  fun findMainClass(project: Project, file: VirtualFile): String?

  companion object {
    @JvmField
    val EP_NAME = ExtensionPointName.create<MainFileProvider>("Educational.mainFileProvider")

    fun getMainClassName(project: Project, file: VirtualFile): String? {
      return EP_NAME.extensionList.mapNotNull { it.findMainClass(project, file) }.firstOrNull()
    }
  }
}