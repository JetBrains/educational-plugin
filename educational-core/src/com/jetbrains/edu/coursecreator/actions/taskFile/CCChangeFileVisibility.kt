package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.pathRelativeToTask
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager
import org.jetbrains.annotations.NonNls
import java.util.function.Supplier

class CCMakeVisibleToLearner : CCChangeFileVisibility(EduCoreBundle.lazyMessage("action.make.visible.to.learner.title"), true) {
  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.ShowToStudent"
  }
}

class CCHideFromLearner : CCChangeFileVisibility(EduCoreBundle.lazyMessage("action.hide.from.learner.title"), false) {
  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.HideFromStudent"
  }
}

abstract class CCChangeFileVisibility(
  val name: Supplier<@ActionText String>,
  val requiredVisibility: Boolean
) : CCChangeFilePropertyActionBase(name) {

  constructor(@ActionText name: String, requiredVisibility: Boolean)
    : this(Supplier { name }, requiredVisibility)

  override fun createStateForFile(project: Project, task: Task, file: VirtualFile): State? {
    val taskRelativePath = file.pathRelativeToTask(project)
    val taskFile = task.getTaskFile(taskRelativePath)
    if (taskFile != null) {
      return FileState(taskFile, file, requiredVisibility)
    }
    return null
  }

  override fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean {
    val path = file.pathRelativeToTask(project)
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
      PlaceholderHighlightingManager.showPlaceholders(project, taskFile)
    }
    else {
      PlaceholderHighlightingManager.hidePlaceholders(project, taskFile.answerPlaceholders)
    }
  }
}
