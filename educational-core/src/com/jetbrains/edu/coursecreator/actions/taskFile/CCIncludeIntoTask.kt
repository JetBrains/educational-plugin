package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.canBeAddedToTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCIncludeIntoTask
  : CCChangeFilePropertyActionBase(EduCoreBundle.lazyMessage("action.include.into.task.title")) {

  override fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean =
    file.canBeAddedToTask(project)

  override fun createStateForFile(project: Project, task: Task, file: VirtualFile): State? {
    if (!file.canBeAddedToTask(project)) return null
    val info = file.fileInfo(project) as? FileInfo.FileInTask ?: return null
    return IncludeFileIntoTask(info)
  }
}

private class IncludeFileIntoTask(private val info: FileInfo.FileInTask) : State {

  override fun changeState(project: Project) {
    info.task.addTaskFile(info.pathInTask)
  }

  override fun restoreState(project: Project) {
    info.task.taskFiles.remove(info.pathInTask)
  }
}
