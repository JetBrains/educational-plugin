package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.application.writeAction
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
  override suspend fun update(project: Project) {
    // TODO EDU-6756 what if lesson was created in the middle of the section?
    localContainer.addItem(remoteItem)
    remoteItem.init(localContainer, false)

    val parentDir = localContainer.getDir(project.courseDir) ?: error("Failed to find parent dir: ${localContainer.name}")
    withContext(Dispatchers.IO) {
      blockingContext {
        GeneratorUtils.createLesson(project, remoteItem, parentDir)
      }
    }

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
  override suspend fun update(project: Project) {
    val parentContainer = localItem.parent
    taskUpdates.forEach {
      it.update(project)
    }
    localItem.init(parentContainer, false)

    if (localItem.name != remoteItem.name) {
      val courseDir = project.courseDir
      val fromDir = localItem.getDir(courseDir) ?: error("Lesson dir wasn't found")
      val parentDir = parentContainer.getDir(courseDir) ?: error("Parent dir wasn't found")

      localItem.name = remoteItem.name
      withContext(Dispatchers.IO) {
        val toDir = blockingContext { GeneratorUtils.createUniqueDir(parentDir, localItem) }
        writeAction {
          fromDir.children.forEach { it.move(this, toDir) }
          fromDir.delete(this)
        }
      }
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(localItem)
    }
  }
}

data class LessonDeletionInfo(override val localItem: Lesson) : LessonUpdate(localItem, null) {
  override suspend fun update(project: Project) {
    val parentContainer = localItem.parent
    parentContainer.removeItem(localItem)
    localItem.deleteFilesOnDisc(project)
  }
}