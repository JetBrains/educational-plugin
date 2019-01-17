package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.fileInfo

class UserCreatedFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun fileDeleted(event: VirtualFileEvent) {
    val (task, pathInTask) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    task.taskFiles.remove(pathInTask)
  }

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    taskFile.isUserCreated = true
  }
}
