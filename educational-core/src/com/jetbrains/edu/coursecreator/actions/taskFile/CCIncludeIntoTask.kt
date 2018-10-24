package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.FileKind.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileInfo

class CCIncludeIntoTask : CCChangeFilePropertyActionBase("Include into Task") {

  override fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean =
    EduUtils.canBeAddedToTask(project, file)

  override fun createStateForFile(project: Project, task: Task, file: VirtualFile): State? {
    if (!EduUtils.canBeAddedToTask(project, file)) return null
    val info = file.fileInfo(project) as? FileInfo.FileInTask ?: return null
    return IncludeFileIntoTask(info)
  }
}

private class IncludeFileIntoTask(private val info: FileInfo.FileInTask) : State {

  override fun changeState(project: Project) {
    when (info.kind) {
      TASK_FILE -> info.task.addTaskFile(info.pathInTask)
      TEST_FILE -> info.task.addTestsTexts(info.pathInTask, "")
      ADDITIONAL_FILE -> info.task.addAdditionalFile(info.pathInTask, "")
    }
  }

  override fun restoreState(project: Project) {
    when (info.kind) {
      TASK_FILE -> info.task.taskFiles.remove(info.pathInTask)
      TEST_FILE -> info.task.testsText.remove(info.pathInTask)
      ADDITIONAL_FILE -> info.task.additionalFiles.remove(info.pathInTask)
    }
  }
}
