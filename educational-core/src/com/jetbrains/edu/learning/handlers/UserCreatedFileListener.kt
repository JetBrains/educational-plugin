package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.FileKind.*
import com.jetbrains.edu.learning.fileInfo

class UserCreatedFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun fileDeleted(event: VirtualFileEvent) {
    val (task, pathInTask, kind) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    when (kind) {
      TASK_FILE -> task.taskFiles.remove(pathInTask)
      ADDITIONAL_FILE -> task.additionalFiles.remove(pathInTask)
      TEST_FILE -> task.testsText.remove(pathInTask)
    }
  }
}
