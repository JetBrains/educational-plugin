package com.jetbrains.edu.coursecreator.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface CCFrameworkLessonManager {
  fun propagateChanges(task: Task)

  companion object {
    fun getInstance(project: Project): CCFrameworkLessonManager = project.service()
  }
}