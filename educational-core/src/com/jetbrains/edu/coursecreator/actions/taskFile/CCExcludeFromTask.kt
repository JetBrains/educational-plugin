package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileInfo

class CCExcludeFromTask : CCChangeFilePropertyActionBase("Exclude from Task") {

  override fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean =
    EduUtils.belongToTask(project, file)

  override fun createStateForFile(project: Project, task: Task, file: VirtualFile): State? {
    if (!EduUtils.belongToTask(project, file)) return null
    val info = file.fileInfo(project) as? FileInfo.FileInTask ?: return null
    return RemoveFileFromTask(info)
  }
}

private class RemoveFileFromTask(private val info: FileInfo.FileInTask) : State {

  private val initialValue: TaskFile = info.task.getFile(info.pathInTask)
      ?: error("Can't find file by `${info.pathInTask}` path in `${info.task.name}` task")

  override fun changeState(project: Project) {
    val taskFile = info.task.taskFiles.remove(info.pathInTask)
    if (taskFile != null) {
      PlaceholderPainter.hidePlaceholders(taskFile)
    }
  }

  override fun restoreState(project: Project) {
    info.task.addTaskFile(initialValue)
    PlaceholderPainter.showPlaceholders(project, initialValue)
  }
}
