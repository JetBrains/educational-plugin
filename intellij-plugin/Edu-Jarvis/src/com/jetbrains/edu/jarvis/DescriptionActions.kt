package com.jetbrains.edu.jarvis

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.actions.DescriptionExecutorAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task

@Service(Service.Level.PROJECT)
class DescriptionActions {

  // TODO: limit the number of description blocks?
  private val actions: MutableMap<Int, MutableSet<DescriptionExecutorAction>> = mutableMapOf()

  fun addAction(task: Task, action: DescriptionExecutorAction) {
    val taskActions = actions[task.id]
    if (taskActions == null) {
      actions[task.id] = mutableSetOf(action)
    } else if (taskActions.none { it.element == action.element }) {
      taskActions.add(action)
    }
  }

  fun getActions(task: Task): Set<DescriptionExecutorAction>? = actions[task.id]

  fun removeAction(task: Task, element: PsiElement) {
    actions[task.id]?.removeIf { it.element == element }
  }

  companion object {
    fun getInstance(project: Project): DescriptionActions {
      return project.service()
    }
  }
}
