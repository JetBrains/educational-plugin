package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson

sealed class LessonUpdate(localItem: Lesson?, remoteItem: Lesson?) : StudyItemUpdate<Lesson>(localItem, remoteItem)

data class LessonCreationInfo(
  val localContainer: ItemContainer,
  override val remoteItem: Lesson
) : LessonUpdate(null, remoteItem) {
  override suspend fun update(project: Project) {
    TODO("Not yet implemented")
  }
}

data class LessonUpdateInfo(
  override val localItem: Lesson,
  override val remoteItem: Lesson,
  val taskUpdates: List<TaskUpdate>
) : LessonUpdate(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    TODO("Not yet implemented")
  }
}

data class LessonDeletionInfo(override val localItem: Lesson) : LessonUpdate(localItem, null) {
  override suspend fun update(project: Project) {
    TODO("Not yet implemented")
  }
}