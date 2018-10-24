package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.FileKind
import com.jetbrains.edu.learning.courseFormat.AdditionalFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileInfo

class CCExcludeFromTask : CCChangeFilePropertyActionBase("Exclude from Task") {

  override fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean =
    EduUtils.belongToTask(project, file)

  override fun createStateForFile(project: Project, task: Task, file: VirtualFile): State? {
    if (!EduUtils.belongToTask(project, file)) return null
    val info = file.fileInfo(project) as? FileInfo.FileInTask ?: return null
    return RemoveFileFromTask(file, info)
  }
}

private class RemoveFileFromTask(
  private val file: VirtualFile,
  private val info: FileInfo.FileInTask
) : State {

  private val initialValue: Any = when (info.kind) {
    FileKind.TASK_FILE -> info.task.getFile(info.pathInTask)
    FileKind.TEST_FILE -> info.task.testsText[info.pathInTask]
    FileKind.ADDITIONAL_FILE -> info.task.additionalFiles[info.pathInTask]
  } ?: error("Can't find file by `${info.pathInTask}` path in `${info.task.name}` task")

  override fun changeState(project: Project) {
    when (info.kind) {
      FileKind.TASK_FILE -> {
        val taskFile = info.task.taskFiles.remove(info.pathInTask)
        if (taskFile != null) {
          CCUtils.hidePlaceholders(project, taskFile, file)
        }
      }
      FileKind.TEST_FILE -> info.task.testsText.remove(info.pathInTask)
      FileKind.ADDITIONAL_FILE -> info.task.additionalFiles.remove(info.pathInTask)
    }
  }

  override fun restoreState(project: Project) {
    when (info.kind) {
      FileKind.TASK_FILE -> {
        info.task.addTaskFile(initialValue as TaskFile)
        CCUtils.showPlaceholders(project, initialValue, file)
      }
      FileKind.TEST_FILE -> info.task.addTestsTexts(info.pathInTask, initialValue as String)
      FileKind.ADDITIONAL_FILE -> info.task.addAdditionalFile(info.pathInTask, initialValue as AdditionalFile)
    }
  }
}
