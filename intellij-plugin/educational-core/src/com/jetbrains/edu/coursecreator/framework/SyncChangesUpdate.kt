package com.jetbrains.edu.coursecreator.framework

import com.intellij.util.ui.update.Update
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

sealed class SyncChangesUpdate : Update(Any(), false)

abstract class SyncChangesTaskFilesUpdate(val task: Task, val taskFiles: Set<TaskFile>) : SyncChangesUpdate() {
  override fun canEat(update: Update): Boolean {
    if (super.canEat(update)) return true
    if (update !is SyncChangesTaskFilesUpdate) return false
    return taskFiles.containsAll(update.taskFiles)
  }

  companion object {
    operator fun invoke(task: Task, taskFiles: Set<TaskFile>, action: () -> Unit) = object : SyncChangesTaskFilesUpdate(task, taskFiles) {
      override fun run() = action()
    }
  }
}

abstract class SyncChangesTaskUpdate(val task: Task) : SyncChangesUpdate() {
  override fun canEat(update: Update): Boolean {
    if (super.canEat(update)) return true
    return when (update) {
      is SyncChangesTaskFilesUpdate -> update.task == task
      is SyncChangesTaskUpdate -> update.task == task
      else -> false
    }
  }

  companion object {
    operator fun invoke(task: Task, action: () -> Unit) = object : SyncChangesTaskUpdate(task) {
      override fun run() = action()
    }
  }
}

abstract class SyncChangesLessonUpdate(val lesson: Lesson) : SyncChangesUpdate() {
  override fun canEat(update: Update): Boolean {
    if (super.canEat(update)) return true
    return when (update) {
      is SyncChangesTaskFilesUpdate -> update.task.lesson == lesson
      is SyncChangesTaskUpdate -> update.task.lesson == lesson
      is SyncChangesLessonUpdate -> lesson == update.lesson
      else -> false
    }
  }

  companion object {
    operator fun invoke(lesson: Lesson, action: () -> Unit) = object : SyncChangesLessonUpdate(lesson) {
      override fun run() = action()
    }
  }
}


