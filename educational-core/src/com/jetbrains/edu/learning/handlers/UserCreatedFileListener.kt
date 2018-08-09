package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener.FileKind.*

class UserCreatedFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun fileDeleted(event: VirtualFileEvent) {
    val (task, pathInTask, kind) = event.fileInfo(project) as? FileInfo.FileInTask ?: return
    when (kind) {
      TASK_FILE -> task.getTaskFiles().remove(pathInTask)
      ADDITIONAL_FILE -> task.additionalFiles.remove(pathInTask)
      TEST_FILE -> task.testsText.remove(pathInTask)
    }
  }
}
