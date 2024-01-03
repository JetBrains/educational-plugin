package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.update.StudyItemUpdater.Companion.deleteFilesOnDisc
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class LessonUpdate(localItem: Lesson?, remoteItem: Lesson?) : StudyItemUpdate<Lesson>(localItem, remoteItem)

data class LessonCreationInfo(
  val localContainer: ItemContainer,
  override val remoteItem: Lesson
) : LessonUpdate(null, remoteItem) {
  @Suppress("UnstableApiUsage")
  override suspend fun update(project: Project, doOperationsOnDisk: Boolean) {
    localContainer.addItem(remoteItem)
    remoteItem.parent = localContainer
    val parentDir = localContainer.getDir(project.courseDir) ?: error("Failed to find parent dir: ${localContainer.name}")
    withContext(Dispatchers.IO) {
      blockingContext {
        GeneratorUtils.createLesson(project, remoteItem, parentDir)
      }
    }
    remoteItem.init(localContainer, false)
    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class LessonUpdateInfo(
  override val localItem: Lesson,
  override val remoteItem: Lesson,
  val taskUpdates: List<TaskUpdate>
) : LessonUpdate(localItem, remoteItem) {
  @Suppress("UnstableApiUsage")
  override suspend fun update(project: Project, doOperationsOnDisk: Boolean) {
    remoteItem.apply {
      index = localItem.index
      parent = localItem.parent
    }
    val parent = localItem.parent
    parent.apply {
      removeItem(localItem)
      addItem(remoteItem.index - 1, remoteItem)
    }
    taskUpdates.forEach {
      it.update(project, false)
    }

    localItem.deleteFilesOnDisc(project)
    val parentDir = parent.getDir(project.courseDir) ?: error("Parent dir wasn't found")
    withContext(Dispatchers.IO) {
      blockingContext {
        GeneratorUtils.createLesson(project, remoteItem, parentDir)
      }
    }
    remoteItem.init(parent, false)

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class LessonDeletionInfo(override val localItem: Lesson) : LessonUpdate(localItem, null) {
  override suspend fun update(project: Project, doOperationsOnDisk: Boolean) {
    localItem.deleteFilesOnDisc(project)
    val parentContainer = localItem.parent
    parentContainer.removeItem(localItem)
    localItem.init(parentContainer, false)
    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(parentContainer)
    }
  }
}