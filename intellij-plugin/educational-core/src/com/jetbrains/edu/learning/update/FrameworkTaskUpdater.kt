package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.update.elements.TaskUpdate

abstract class FrameworkTaskUpdater(project: Project, lesson: FrameworkLesson) : TaskUpdaterBase<FrameworkLesson>(project, lesson) {

  override suspend fun collect(localItems: List<Task>, remoteItems: List<Task>): List<TaskUpdate> {
    return emptyList()
  }
}