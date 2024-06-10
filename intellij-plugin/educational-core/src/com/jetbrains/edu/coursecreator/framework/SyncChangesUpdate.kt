package com.jetbrains.edu.coursecreator.framework

import com.intellij.util.ui.update.Update
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Base class for sync changes updates.
 */
sealed class SyncChangesUpdate(priority: Int) : Update(Any(), false, priority)

/**
 * Class for updating state for a list of task files in a given task
 * High priority, since all task files updates must be executed earlier than study item state updates
 */
abstract class TaskFilesSyncChangesUpdate(val task: Task, val taskFiles: Set<TaskFile>) : SyncChangesUpdate(HIGH_PRIORITY) {
  constructor(task: Task) : this(task, task.taskFiles.values.toSet())

  override fun canEat(update: Update): Boolean {
    if (super.canEat(update)) return true
    if (update !is TaskFilesSyncChangesUpdate) return false
    return task == update.task && taskFiles.containsAll(update.taskFiles)
  }

  companion object {
    operator fun invoke(task: Task, taskFiles: Set<TaskFile>, action: () -> Unit) = object : TaskFilesSyncChangesUpdate(task, taskFiles) {
      override fun run() = action()
    }

    operator fun invoke(task: Task, action: () -> Unit) = object : TaskFilesSyncChangesUpdate(task) {
      override fun run() = action()
    }
  }
}

/**
 * Base class for updating sync changes state for a given study item
 * Lower priority, since all task files updates must be executed earlier than study item state updates
 */
sealed class StudyItemSyncChangesUpdate<T : StudyItem>(priority: Int, val item: T) : SyncChangesUpdate(priority)

/**
 * Class for updating state for a given task
 */
abstract class TaskSyncChangesUpdate(task: Task) : StudyItemSyncChangesUpdate<Task>(LOW_PRIORITY, task) {
  override fun canEat(update: Update): Boolean {
    if (super.canEat(update)) return true
    if (update !is TaskSyncChangesUpdate) return false
    return item == update.item
  }

  companion object {
    operator fun invoke(task: Task, action: () -> Unit) = object : TaskSyncChangesUpdate(task) {
      override fun run() = action()
    }
  }
}

/**
 * Class for updating state for a given lesson
 * The priority is lower than [TaskSyncChangesUpdate], since all events must be executed later than [TaskSyncChangesUpdate]
 */
abstract class LessonSyncChangesUpdate(lesson: Lesson) : StudyItemSyncChangesUpdate<Lesson>(LOW_PRIORITY + 1, lesson) {
  override fun canEat(update: Update): Boolean {
    if (super.canEat(update)) return true
    if (update !is LessonSyncChangesUpdate) return false
    return item == update.item
  }

  companion object {
    operator fun invoke(lesson: Lesson, action: () -> Unit) = object : LessonSyncChangesUpdate(lesson) {
      override fun run() = action()
    }
  }
}

/**
 * Base class for updating project UI for sync changes state
 * Lowest priority, since all other updates must be executed
 */
abstract class ProjectSyncChangesUpdate : SyncChangesUpdate(LOW_PRIORITY + 2) {
  override fun canEat(update: Update): Boolean {
    if (super.canEat(update)) return true
    return update is ProjectSyncChangesUpdate
  }

  companion object {
    operator fun invoke(action: () -> Unit) = object : ProjectSyncChangesUpdate() {
      override fun run() = action()
    }
  }
}