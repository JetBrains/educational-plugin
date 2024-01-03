package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.update.HyperskillItemUpdater
import com.jetbrains.edu.learning.update.LessonUpdater
import com.jetbrains.edu.learning.update.TaskUpdater

class HyperskillLessonUpdater(
  project: Project,
  container: LessonContainer
) : LessonUpdater(project, container), HyperskillItemUpdater<Lesson> {
  override fun createTaskUpdater(lesson: Lesson): TaskUpdater = HyperskillTaskUpdater(project, lesson)
}