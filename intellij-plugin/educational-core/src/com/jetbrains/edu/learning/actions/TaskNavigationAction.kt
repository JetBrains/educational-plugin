package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AiAssistantState
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.initAiHintContext
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel

abstract class TaskNavigationAction : DumbAwareAction() {

  protected open fun getCustomAction(task: Task): ((Project, Task) -> Unit)? = null

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (!project.isEduProject()) return
    navigateTask(project, e.place)

    // Save context for the AI edu assistant
    // TODO: should we do it again somewhere if the course content was changed?
    val state = project.eduState ?: return
    initAiHintContext(state.task, AiAssistantState.ContextInitialized)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = false
    val project = e.project ?: return
    if (!project.isEduProject()) return
    val currentTask = TaskToolWindowView.getInstance(project).currentTask ?: return
    if (getTargetTask(currentTask) != null || getCustomAction(currentTask) != null) {
      e.presentation.isEnabled = true
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun navigateTask(project: Project, place: String) {
    val currentTask = TaskToolWindowView.getInstance(project).currentTask ?: return
    val customAction = getCustomAction(currentTask)
    if (customAction != null) {
      customAction(project, currentTask)
      return
    }
    val targetTask = getTargetTask(currentTask) ?: return

    NavigationUtils.navigateToTask(project, targetTask, currentTask)
    EduCounterUsageCollector.taskNavigation(
      if (place == CheckPanel.ACTION_PLACE) EduCounterUsageCollector.TaskNavigationPlace.CHECK_PANEL
      else EduCounterUsageCollector.TaskNavigationPlace.TASK_DESCRIPTION_TOOLBAR
    )
  }

  protected abstract fun getTargetTask(sourceTask: Task): Task?
}
