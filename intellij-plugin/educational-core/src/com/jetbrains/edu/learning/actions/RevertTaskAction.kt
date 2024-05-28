package com.jetbrains.edu.learning.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.EmptyIcon
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.actions.EduActionUtils.updateAction
import com.jetbrains.edu.learning.courseFormat.ext.revertTaskFiles
import com.jetbrains.edu.learning.courseFormat.ext.revertTaskParameters
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager.updateDependentPlaceholders
import com.jetbrains.edu.learning.projectView.ProgressUtil.updateCourseProgress
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.Companion.revertTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting

class RevertTaskAction : DumbAwareAction(), RightAlignedToolbarAction {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    if (task.isChangedOnFailed) {
      return
    }
    val result = MessageDialogBuilder.yesNo(EduCoreBundle.message("action.Educational.RefreshTask.text"),
                                            EduCoreBundle.message("action.Educational.RefreshTask.progress.dropped")).ask(project)
    if (!result) return
    revert(project, task)
    revertTask()
  }

  override fun update(e: AnActionEvent) {
    updateAction(e)
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    if (
      !task.course.isStudy ||
      task.isChangedOnFailed // we disable revert action for tasks with changing on error
    ) {
      val presentation = e.presentation
      presentation.isEnabledAndVisible = false
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.RefreshTask"

    val EP_NAME: ExtensionPointName<RevertTaskExtension> = ExtensionPointName.create("Educational.revertTaskExtension")

    @VisibleForTesting
    fun revert(project: Project, task: Task) {
      task.apply {
        revertTaskFiles(project)
        revertTaskParameters()
        YamlFormatSynchronizer.saveItem(this)
      }

      updateDependentPlaceholders(project, task)

      EP_NAME.forEachExtensionSafe {
        it.onTaskReversion(project, task)
      }
      EditorNotifications.getInstance(project).updateAllNotifications()
      EduNotificationManager.showInfoNotification(
        project = project,
        content = EduCoreBundle.message("action.Educational.RefreshTask.result")
      ) {
        setIcon(EmptyIcon.ICON_16)
      }
      ProjectView.getInstance(project).refresh()
      TaskToolWindowView.getInstance(project).updateTaskSpecificPanel()
      TaskToolWindowView.getInstance(project).readyToCheck()
      updateCourseProgress(project)
    }
  }

  fun interface RevertTaskExtension {
    fun onTaskReversion(project: Project, task: Task)
  }
}