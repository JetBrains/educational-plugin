package com.jetbrains.edu.learning.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.FLTaskState

interface FrameworkLessonManager : EduTestAware {
  fun prepareNextTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean)
  fun preparePrevTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean)

  fun saveExternalChanges(task: Task, externalState: FLTaskState)
  fun updateUserChanges(task: Task, newInitialState: FLTaskState)

  fun getChangesTimestamp(task: Task): Long

  /**
   * Retrieves the state of a given task in a framework lesson.
   *
   * @param lesson the framework lesson containing the task
   * @param task the task for which to retrieve the state
   * @return a map representing the state of the task, where the keys are the file paths, and the values are the file contents
   */
  fun getTaskState(lesson: FrameworkLesson, task: Task): FLTaskState

  companion object {
    fun getInstance(project: Project): FrameworkLessonManager = project.service()
  }
}
