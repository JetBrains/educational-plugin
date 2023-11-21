package com.jetbrains.edu.coursecreator.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface CCFrameworkLessonManager {
  /**
   * Tries to merge changes from the last saved state of [task] until current state to all subsequent tasks
   * and saves changes for them.
   *
   * If the changes cannot be merged automatically, the merge dialog is displayed.
   */
  fun propagateChanges(task: Task)

  /**
   * Saves the current file state of the [task] to the storage and updates the record of the task.
   */
  fun saveCurrentState(task: Task)

  companion object {
    fun getInstance(project: Project): CCFrameworkLessonManager = project.service()
  }
}