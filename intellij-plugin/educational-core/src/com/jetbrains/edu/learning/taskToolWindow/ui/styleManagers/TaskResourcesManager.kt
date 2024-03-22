package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers

import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface TaskResourcesManager<T : Task> {

  fun getText(task: T): String
}