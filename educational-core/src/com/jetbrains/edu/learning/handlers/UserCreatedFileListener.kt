package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty

class UserCreatedFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun fileDeleted(fileInfo: FileInfo, file: VirtualFile) {
    val (task, pathInTask) = fileInfo as? FileInfo.FileInTask ?: return
    if (!task.shouldBeEmpty(pathInTask)) {
      task.removeTaskFile(pathInTask)
    }
  }

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    taskFile.isLearnerCreated = true
  }
}
