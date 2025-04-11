package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.update.elements.LessonCreationInfo
import com.jetbrains.edu.learning.update.elements.LessonDeletionInfo
import com.jetbrains.edu.learning.update.elements.LessonUpdate
import com.jetbrains.edu.learning.update.elements.LessonUpdateInfo

abstract class LessonUpdater(project: Project, private val container: LessonContainer) : StudyItemUpdater<Lesson, LessonUpdate>(project) {
  protected abstract fun createTaskUpdater(lesson: Lesson): TaskUpdater
  protected abstract fun createFrameworkTaskUpdater(lesson: FrameworkLesson): FrameworkTaskUpdater

  suspend fun collect(remoteContainer: ItemContainer): List<LessonUpdate> {
    val localLessons = container.items.filterIsInstance<Lesson>()
    val remoteLessons = remoteContainer.items.filterIsInstance<Lesson>()
    return collect(localLessons, remoteLessons)
  }

  override suspend fun collect(localItems: List<Lesson>, remoteItems: List<Lesson>): List<LessonUpdate> {
    // We assume that standard and framework lessons are updated independently,
    // so we don't support the situation when a remote lesson became framework or vice versa.
    val (localFrameworkLessons, localLessons) = localItems.partition { it is FrameworkLesson }
    val (remoteFrameworkLessons, remoteLessons) = remoteItems.partition { it is FrameworkLesson }

    val frameworkLessonUpdates = collectLessonUpdates(localFrameworkLessons, remoteFrameworkLessons, isFramework = true)
    val lessonUpdates = collectLessonUpdates(localLessons, remoteLessons, isFramework = false)

    return frameworkLessonUpdates + lessonUpdates
  }

  /**
   * If [isFramework] is true, both lists must contain [FrameworkLesson].
   */
  private suspend fun collectLessonUpdates(localItems: List<Lesson>, remoteItems: List<Lesson>, isFramework: Boolean): List<LessonUpdate> {
    val updates = mutableListOf<LessonUpdate>()

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

      // lessons to be updated
      val localLesson = localLessons.firstOrNull() ?: continue
      val remoteLesson = remoteLessons.find { it.id == localLesson.id }
      if (remoteLesson == null) {
        updates.add(LessonDeletionInfo(localLesson))
        localLessons.remove(localLesson)
      }
      else {
        val taskUpdater = if (isFramework) {
          createFrameworkTaskUpdater(localLesson as FrameworkLesson)
        }
        else {
          createTaskUpdater(localLesson)
        }
        val taskUpdates = taskUpdater.collect(remoteLesson)
        if (taskUpdates.isNotEmpty() || localLesson.isOutdated(remoteLesson) || localLesson.isChanged(remoteLesson)) {
          updates.add(LessonUpdateInfo(localLesson, remoteLesson, taskUpdates))
        }

        localLessons.remove(localLesson)
        remoteLessons.remove(remoteLesson)
      }
    }

    return updates
  }

  private fun Lesson.isChanged(remoteLesson: Lesson): Boolean = when {
    name != remoteLesson.name -> true
    index != remoteLesson.index -> true
    taskList.size != remoteLesson.taskList.size -> true
    else -> false
  }
}