package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.belongsToTask
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager
import org.jetbrains.annotations.NonNls

class CCExcludeFromTask : CCChangeFilePropertyActionBase(EduCoreBundle.lazyMessage("action.exclude.from.task.title")) {

  override fun isAvailableForSingleFile(project: Project, task: Task?, file: VirtualFile): Boolean =
    file.belongsToTask(project)

  override fun createStateForFile(project: Project, task: Task?, file: VirtualFile): State? {
    if (!file.belongsToTask(project)) return null
    val info = file.fileInfo(project) as? FileInfo.FileInTask ?: return null
    return RemoveFileFromTask(info)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.ExcludeFromTask"
  }
}

private class RemoveFileFromTask(private val info: FileInfo.FileInTask) : State {

  private val initialValue: TaskFile = info.task.getTaskFile(info.pathInTask) ?: error(errorMessage(info))
  private val initialIndex: Int = info.task.taskFileIndex(info.pathInTask) ?: error(errorMessage(info))

  override fun changeState(project: Project) {
    val taskFile = info.task.removeTaskFile(info.pathInTask)
    if (taskFile != null) {
      PlaceholderHighlightingManager.hidePlaceholders(project, taskFile.answerPlaceholders)
    }
  }

  override fun restoreState(project: Project) {
    info.task.addTaskFile(initialValue, initialIndex)
    PlaceholderHighlightingManager.showPlaceholders(project, initialValue)
  }

  companion object {
    private fun errorMessage(info: FileInfo.FileInTask) = "Can't find file by `${info.pathInTask}` path in `${info.task.name}` task"
  }
}
