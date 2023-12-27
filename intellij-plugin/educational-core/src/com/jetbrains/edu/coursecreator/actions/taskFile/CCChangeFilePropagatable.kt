package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.pathRelativeToTask
import org.jetbrains.annotations.NonNls
import java.util.function.Supplier

class CCAllowFilePropagation : CCChangeFilePropagationFlag(EduCoreBundle.lazyMessage("action.Educational.Educator.AllowFilePropagation.text"), true) {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.AllowFilePropagation"
  }
}

class CCIgnoreFilePropagation : CCChangeFilePropagationFlag(EduCoreBundle.lazyMessage("action.Educational.Educator.IgnoreFilePropagation.text"), false) {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.IgnoreFilePropagation"
  }
}

abstract class CCChangeFilePropagationFlag(
  val name: Supplier<@NlsActions.ActionText String>,
  val requiredPropagationFlag: Boolean
) : CCChangeFilePropertyActionBase(name) {
  override fun update(e: AnActionEvent) {
    if (!isFeatureEnabled(EduExperimentalFeatures.CC_FL_APPLY_CHANGES)) {
      return
    }
    super.update(e)
  }

  override fun createStateForFile(project: Project, task: Task, file: VirtualFile): State? {
    val taskRelativePath = file.pathRelativeToTask(project)
    val taskFile = task.getTaskFile(taskRelativePath)
    if (taskFile != null) {
      return FileState(taskFile, requiredPropagationFlag)
    }
    return null
  }

  override fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean {
    if (task.parent !is FrameworkLesson) return false
    val path = file.pathRelativeToTask(project)
    val propagatableFile = task.getTaskFile(path)
    return propagatableFile?.isPropagatable == !requiredPropagationFlag
  }

  private class FileState(
    val taskFile: TaskFile,
    val isPropagatable: Boolean
  ) : State {

    val initialPropagatableFlag: Boolean = taskFile.isPropagatable

    override fun changeState(project: Project) {
      taskFile.isPropagatable = isPropagatable
    }

    override fun restoreState(project: Project) {
      taskFile.isPropagatable = initialPropagatableFlag
    }
  }
}