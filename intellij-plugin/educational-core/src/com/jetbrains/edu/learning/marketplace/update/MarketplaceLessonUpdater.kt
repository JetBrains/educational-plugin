package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.update.FrameworkTaskUpdater
import com.jetbrains.edu.learning.update.LessonUpdater
import com.jetbrains.edu.learning.update.MarketplaceItemUpdater
import com.jetbrains.edu.learning.update.TaskUpdater

class MarketplaceLessonUpdater(
  project: Project,
  container: LessonContainer
) : LessonUpdater(project, container), MarketplaceItemUpdater<Lesson> {
  override fun createTaskUpdater(lesson: Lesson): TaskUpdater = MarketplaceTaskUpdater(project, lesson)
  override fun createFrameworkTaskUpdater(lesson: FrameworkLesson): FrameworkTaskUpdater = MarketplaceFrameworkTaskUpdater(project, lesson)
}