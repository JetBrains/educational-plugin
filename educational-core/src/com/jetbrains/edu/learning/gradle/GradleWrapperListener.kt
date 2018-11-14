package com.jetbrains.edu.learning.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX

class GradleWrapperListener(private val project: Project) : VirtualFileListener {

  override fun fileCreated(event: VirtualFileEvent) {
    if (project.isDisposed) return
    if (StudyTaskManager.getInstance(project).course == null) return
    if (event.file.name == GRADLE_WRAPPER_UNIX) {
      VfsUtil.virtualToIoFile(event.file).setExecutable(true)
      // We don't need this listener anymore
      VirtualFileManager.getInstance().removeVirtualFileListener(this)
    }
  }
}
