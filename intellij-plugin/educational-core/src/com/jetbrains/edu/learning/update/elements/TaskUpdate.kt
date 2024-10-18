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
  override suspend fun update(project: Project) {
    // TODO EDU-6756 what if task was created in the middle of the lesson?
    localLesson.addItem(remoteItem)
    remoteItem.init(localLesson, false)

    val lessonDir = localLesson.getDir(project.courseDir) ?: error("Failed to find lesson dir: ${localLesson.name}")
    withContext(Dispatchers.IO) {
      blockingContext {
        GeneratorUtils.createTask(project, remoteItem, lessonDir)
      }
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class TaskUpdateInfo(override val localItem: Task, override val remoteItem: Task) : TaskUpdate(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    val lesson = localItem.parent
    if (lesson is FrameworkLesson) {
      // TODO this case will be implemented in EDU-6560
      return
    }

    lesson.removeItem(localItem)
    localItem.deleteFilesOnDisc(project)

    remoteItem.apply {
      // TODO EDU-6756 maybe drop it, as index will be calculated in LessonUpdate .init()
      index = localItem.index
      // we keep CheckStatus.Solved for task even if it was updated
      // we keep CheckStatus.Failed for task only if it was not updated
      if (localItem.status != CheckStatus.Failed) {
        status = localItem.status
      }
      init(lesson, false)
    }
    val lessonDir = lesson.getDir(project.courseDir) ?: error("Lesson dir wasn't found")
    withContext(Dispatchers.IO) {
      blockingContext {
        EduCourseUpdater.createTaskDirectories(project, lessonDir, remoteItem)
      }
    }
    lesson.addItem(localItem.index - 1, remoteItem)

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class TaskDeletionInfo(override val localItem: Task) : TaskUpdate(localItem, null) {
  override suspend fun update(project: Project) {
    val lesson = localItem.parent
    lesson.removeItem(localItem)
    localItem.deleteFilesOnDisc(project)
  }
}