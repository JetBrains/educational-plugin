package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.update.TaskUpdater

class MarketplaceTaskUpdater(project: Project, lesson: Lesson) : TaskUpdater(project, lesson) {
  // For tasks in the marketplace, there is no supported updateDate
  override fun isLocalTaskOutdated(localTask: Task, remoteTask: Task): Boolean = false
}