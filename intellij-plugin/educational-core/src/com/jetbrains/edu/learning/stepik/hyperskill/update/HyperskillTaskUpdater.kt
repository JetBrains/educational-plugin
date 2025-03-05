package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.update.FrameworkTaskUpdater
import com.jetbrains.edu.learning.update.HyperskillItemUpdater
import com.jetbrains.edu.learning.update.TaskUpdater

class HyperskillTaskUpdater(project: Project, lesson: Lesson) :
  TaskUpdater(project, lesson),
  HyperskillItemUpdater<Task>

class HyperskillFrameworkTaskUpdater(project: Project, lesson: FrameworkLesson) :
  FrameworkTaskUpdater(project, lesson),
  HyperskillItemUpdater<Task> {

  override fun Task.canBeUpdatedBy(remoteTask: Task): Boolean = true

  override suspend fun Task.shouldBeUpdated(remoteTask: Task): Boolean = isOutdated(remoteTask)
}