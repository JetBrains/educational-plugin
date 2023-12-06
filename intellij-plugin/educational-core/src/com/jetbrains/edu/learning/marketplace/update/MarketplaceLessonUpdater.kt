package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.update.LessonUpdater
import com.jetbrains.edu.learning.update.TaskUpdater

class MarketplaceLessonUpdater(project: Project, container: ItemContainer) : LessonUpdater(project, container) {
  override fun createTaskUpdater(lesson: Lesson): TaskUpdater = MarketplaceTaskUpdater(project, lesson)
}