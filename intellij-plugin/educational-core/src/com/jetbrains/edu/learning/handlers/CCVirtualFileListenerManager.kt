package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.StudyTaskManager

interface CCVirtualFileListenerManager {
  fun getCCListener(project: Project, manager: StudyTaskManager): CCVirtualFileListener

  companion object {
    val EP_NAME = ExtensionPointName.create<CCVirtualFileListenerManager>("Educational.virtualFileListenerManager")
    fun getCCVirtualFileListener(project: Project, manager: StudyTaskManager) =
      EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.getCCListener(project, manager) ?: CCVirtualFileListener(project, manager)
  }
}