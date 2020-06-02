package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.courseFormat.TaskFile

class UserCreatedFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun fileDeleted(fileInfo: FileInfo, file: VirtualFile) {
    val (task, pathInTask) = fileInfo as? FileInfo.FileInTask ?: return
    task.taskFiles.remove(pathInTask)
  }

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    taskFile.isLearnerCreated = true
  }
}
