package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.handlers.CCVirtualFileListenerManager
import com.jetbrains.edu.learning.StudyTaskManager

class CSharpCCVFileListenerManager : CCVirtualFileListenerManager {
  override fun getCCListener(project: Project, manager: StudyTaskManager): CCVirtualFileListener =
    CSharpCCVirtualFileListener(project, manager)
}