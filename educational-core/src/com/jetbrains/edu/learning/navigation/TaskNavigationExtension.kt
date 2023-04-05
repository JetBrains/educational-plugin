package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Allows performing additional specific preparation after task navigation
 */
interface TaskNavigationExtension {
  fun onTaskNavigation(project: Project, task: Task, fromTask: Task?)

  companion object {
    val EP: ExtensionPointName<TaskNavigationExtension> = ExtensionPointName.create("Educational.taskNavigationExtension")
  }
}
