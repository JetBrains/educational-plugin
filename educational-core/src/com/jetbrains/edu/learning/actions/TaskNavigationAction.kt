package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import javax.swing.Icon

abstract class TaskNavigationAction(
  text: String,
  description: String,
  icon: Icon
) : DumbAwareActionWithShortcut(text, description, icon) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    navigateTask(project)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = false
    val project = e.project ?: return
    val currentTask = EduUtils.getCurrentTask(project) ?: return
    if (getTargetTask(currentTask) != null) {
      e.presentation.isEnabled = true
    }
  }

  private fun navigateTask(project: Project) {
    val currentTask = EduUtils.getCurrentTask(project) ?: return
    val targetTask = getTargetTask(currentTask) ?: return

    NavigationUtils.navigateToTask(project, targetTask, currentTask)
  }

  protected abstract fun getTargetTask(sourceTask: Task): Task?
}
