package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.update.elements.LessonCreationInfo
import com.jetbrains.edu.learning.update.elements.LessonDeletionInfo
import com.jetbrains.edu.learning.update.elements.LessonUpdate
import com.jetbrains.edu.learning.update.elements.LessonUpdateInfo

abstract class LessonUpdater(project: Project, private val container: ItemContainer) : StudyItemUpdater<Lesson, LessonUpdate>(project) {
  protected abstract fun createTaskUpdater(lesson: Lesson): TaskUpdater

  override suspend fun collect(localItems: List<Lesson>, remoteItems: List<Lesson>) {
    val localLessons = localItems.toMutableSet()
    val remoteLessons = remoteItems.toMutableSet()

    while (localLessons.isNotEmpty() || remoteLessons.isNotEmpty()) {
      if (localLessons.isEmpty()) {
        // new lessons
        remoteLessons.forEach { remoteLesson ->
          updates.add(LessonCreationInfo(container, remoteLesson))
        }
        remoteLessons.clear()
      }
      if (remoteLessons.isEmpty()) {
        // lessons to be deleted
        localLessons.forEach { localLesson ->
          updates.add(LessonDeletionInfo(localLesson))
        }
        localLessons.clear()
      }

      // tasks to be updated
      val localLesson = localLessons.firstOrNull() ?: continue
      val remoteLesson = remoteLessons.find { it.id == localLesson.id }
      if (remoteLesson == null) {
        updates.add(LessonDeletionInfo(localLesson))
        localLessons.remove(localLesson)
      }
      else {
        val taskUpdater = createTaskUpdater(localLesson)
        taskUpdater.collect(remoteLesson)
        if (taskUpdater.areUpdatesAvailable()) {
          updates.add(LessonUpdateInfo(localLesson, remoteLesson, taskUpdater))
        }

        localLessons.remove(localLesson)
        remoteLessons.remove(remoteLesson)
      }
    }
  }

  override suspend fun doUpdate() {
    TODO("Not yet implemented")
  }
}