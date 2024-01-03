package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduCourseUpdater
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.update.StudyItemUpdater.Companion.deleteFilesOnDisc
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class TaskUpdate(localItem: Task?, remoteItem: Task?) : StudyItemUpdate<Task>(localItem, remoteItem)

data class TaskCreationInfo(val localLesson: Lesson, override val remoteItem: Task) : TaskUpdate(null, remoteItem) {
  @Suppress("UnstableApiUsage")
  override suspend fun update(project: Project, doOperationsOnDisk: Boolean) {
    localLesson.addTask(remoteItem)
    remoteItem.parent = localLesson

    if (doOperationsOnDisk) {
      val lessonDir = localLesson.getDir(project.courseDir) ?: error("Failed to find lesson dir: ${localLesson.name}")
      withContext(Dispatchers.IO) {
        blockingContext {
          GeneratorUtils.createTask(project, remoteItem, lessonDir)
        }
      }
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class TaskUpdateInfo(override val localItem: Task, override val remoteItem: Task) : TaskUpdate(localItem, remoteItem) {
  @Suppress("UnstableApiUsage")
  override suspend fun update(project: Project, doOperationsOnDisk: Boolean) {
    if (localItem.lesson is FrameworkLesson) {
      // TODO this case will be implemented in EDU-6560
      return
    }
    remoteItem.apply {
      index = localItem.index
      parent = localItem.lesson
      // we keep CheckStatus.Solved for task even if it was updated
      // we keep CheckStatus.Failed for task only if it was not updated
      if (localItem.status != CheckStatus.Failed) {
        status = localItem.status
      }
    }
    val lesson = localItem.parent
    lesson.apply {
      removeItem(localItem)
      addItem(remoteItem.index - 1, remoteItem)
    }

    if (doOperationsOnDisk) {
      localItem.deleteFilesOnDisc(project)
      val lessonDir = lesson.getDir(project.courseDir) ?: error("Lesson dir wasn't found")
      withContext(Dispatchers.IO) {
        blockingContext {
          EduCourseUpdater.createTaskDirectories(project, lessonDir, remoteItem)
        }
      }
      remoteItem.init(lesson, false)
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class TaskDeletionInfo(override val localItem: Task) : TaskUpdate(localItem, null) {
  override suspend fun update(project: Project, doOperationsOnDisk: Boolean) {
    val lesson = localItem.parent
    lesson.removeItem(localItem)

    if (doOperationsOnDisk) {
      localItem.deleteFilesOnDisc(project)
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(lesson)
    }
  }
}