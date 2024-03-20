package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.framework.updateSyncChangesIcon
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.pathRelativeToTask
import org.jetbrains.annotations.NonNls
import java.util.function.Supplier

class CCAllowFileSyncChanges : CCChangeFilePropagationFlag(EduCoreBundle.lazyMessage("action.Educational.Educator.AllowFileToSyncChanges.text"), true) {
  override fun update(e: AnActionEvent) {
    if (!isFeatureEnabled(EduExperimentalFeatures.CC_FL_SYNC_CHANGES)) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    super.update(e)
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun isAvailableForDirectory(project: Project, task: Task, dir: VirtualFile): Boolean = false

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.AllowFileToSyncChanges"
  }
}

class CCIgnoreFileInSyncChanges : CCChangeFilePropagationFlag(EduCoreBundle.lazyMessage("action.Educational.Educator.IgnoreFilePropagation.text"), false) {
  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    if (!isFeatureEnabled(EduExperimentalFeatures.CC_FL_SYNC_CHANGES)) {
      presentation.isEnabledAndVisible = false
      return
    }

    super.update(e)

    if (!presentation.isEnabledAndVisible) return

    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext).orEmpty()
    if (virtualFiles.size > 1 || virtualFiles.singleOrNull()?.isDirectory == true) {
      e.presentation.text = EduCoreBundle.message("action.Educational.Educator.IgnoreFilePropagation.All.text")
      e.presentation.description = EduCoreBundle.message("action.Educational.Educator.IgnoreFilePropagation.All.description")
    }
  }

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
  private val requiredPropagationFlag: Boolean
) : CCChangeFilePropertyActionBase(name) {
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
      update(project)
    }

    override fun restoreState(project: Project) {
      taskFile.isPropagatable = initialPropagatableFlag
      update(project)
    }

    private fun update(project: Project) {
      updateSyncChangesIcon(project, taskFile)
      taskFile.getVirtualFile(project)?.let { file ->
        EditorNotifications.getInstance(project).updateNotifications(file)
      }
    }
  }
}