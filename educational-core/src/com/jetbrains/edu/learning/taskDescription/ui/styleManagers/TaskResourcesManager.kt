package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface TaskResourcesManager<T : Task> {
  val resources: Map<String, String>

  fun getText(task: T): String
}