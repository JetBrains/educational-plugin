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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

sealed class TaskUpdate(localItem: Task?, remoteItem: Task?) : StudyItemUpdate<Task>(localItem, remoteItem)

data class TaskCreationInfo(val localLesson: Lesson, override val remoteItem: Task) : TaskUpdate(null, remoteItem) {
  @Suppress("UnstableApiUsage")
  override suspend fun doUpdate(project: Project) {
    localLesson.addTask(remoteItem)
    remoteItem.parent = localLesson
    val lessonDir = localLesson.getDir(project.courseDir) ?: error("Failed to find lesson dir: ${localLesson.name}")
    withContext(Dispatchers.IO) {
      blockingContext {
        GeneratorUtils.createTask(project, remoteItem, lessonDir)
      }
    }
  }
}

data class TaskUpdateInfo(override val localItem: Task, override val remoteItem: Task) : TaskUpdate(localItem, remoteItem) {
  override suspend fun doUpdate(project: Project) {
    if (localItem.lesson is FrameworkLesson) {
      // TODO this case will be implemented in EDU-6241
      return
    }
    remoteItem.apply {
      // TODO EDU-6241 recalculate task index on lesson update
      index = localItem.index
      parent = localItem.lesson
      // we keep CheckStatus.Solved for task even if it was updated
      // we keep CheckStatus.Failed for task only if it was not updated
      if (localItem.status != CheckStatus.Failed) {
        status = localItem.status
      }
    }
    withContext(Dispatchers.IO) {
      val localTask = localItem
      val remoteTask = remoteItem
      localTask.deleteFilesOnDisc(project)
      val lesson = localTask.parent
      remoteTask.init(lesson, false)
      val lessonDir = lesson.getDir(project.courseDir) ?: error("Lesson dir wasn't found")
      EduCourseUpdater.createTaskDirectories(project, lessonDir, remoteTask)
    }
    localItem.parent.apply {
      removeItem(localItem)
      addItem(remoteItem.index - 1, remoteItem)
    }
    runBlocking {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class TaskDeletionInfo(override val localItem: Task) : TaskUpdate(localItem, null) {
  override suspend fun doUpdate(project: Project) {
    localItem.deleteFilesOnDisc(project)
    val lesson = localItem.parent
    lesson.removeItem(localItem)
    runBlocking {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(lesson)
    }
  }
}