package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.jetbrains.edu.learning.EduUtils.*

class UserCreatedFileListener(private val project: Project) : VirtualFileListener {

  override fun fileCreated(event: VirtualFileEvent) {
    if (project.isDisposed) return
    val createdFile = event.file
    if (canBeAddedAsTaskFile(project, createdFile)) {
      val task = getTaskForFile(project, createdFile) ?: error("Can't find task for `$createdFile`")
      val path = pathRelativeToTask(project, createdFile)
      task.addTaskFile(path).isUserCreated = true
    }
  }

  override fun fileDeleted(event: VirtualFileEvent) {
    if (project.isDisposed) return
    val removedFile = event.file
    val taskFile = getTaskFile(project, removedFile) ?: return
    val task = taskFile.task
    task.getTaskFiles().remove(pathRelativeToTask(project, removedFile))
  }
}
