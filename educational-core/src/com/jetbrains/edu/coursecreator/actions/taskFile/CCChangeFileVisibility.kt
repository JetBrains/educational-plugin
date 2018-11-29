package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CCMakeVisibleToStudent : CCChangeFileVisibility("Make Visible to Student", true)
class CCHideFromStudent : CCChangeFileVisibility("Hide from Student", false)

abstract class CCChangeFileVisibility(val name: String, val requiredVisibility: Boolean) : CCChangeFilePropertyActionBase(name) {

  override fun createStateForFile(project: Project, task: Task, file: VirtualFile): State? {
    val taskRelativePath = EduUtils.pathRelativeToTask(project, file)
    val taskFile = task.getTaskFile(taskRelativePath)
    if (taskFile != null) {
      return FileState(taskFile, file, requiredVisibility)
    }
    return null
  }

  override fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean {
    val path = EduUtils.pathRelativeToTask(project, file)
    val visibleFile = task.getTaskFile(path)
    return visibleFile?.isVisible == !requiredVisibility
  }
}

private class FileState(
  val taskFile: TaskFile,
  val file: VirtualFile,
  val visibility: Boolean
) : State {

  val initialVisibility: Boolean = taskFile.isVisible
  val placeholders: List<AnswerPlaceholder> = taskFile.answerPlaceholders

  override fun changeState(project: Project) {
    taskFile.isVisible = visibility
    onVisibilityChange(project, taskFile, file, visibility)
    taskFile.answerPlaceholders = mutableListOf()
  }

  override fun restoreState(project: Project) {
    taskFile.isVisible = initialVisibility
    taskFile.answerPlaceholders = placeholders
    onVisibilityChange(project, taskFile, file, initialVisibility)
  }

  private fun onVisibilityChange(project: Project, taskFile: TaskFile, file: VirtualFile, visibility: Boolean) {
    if (taskFile.answerPlaceholders.isEmpty() || !FileEditorManager.getInstance(project).isFileOpen(file)) return
    if (visibility) {
      PlaceholderPainter.showPlaceholders(project, taskFile)
    } else {
      PlaceholderPainter.hidePlaceholders(taskFile)
    }
  }
}
