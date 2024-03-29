package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.TaskNavigationExtension

class SqlTaskNavigationExtension : TaskNavigationExtension {
  override fun onTaskNavigation(project: Project, task: Task, fromTask: Task?) {
    val lesson = task.lesson
    if (lesson is FrameworkLesson && lesson.course.isStudy) {
      attachSqlConsoleForOpenFiles(project, task)
      // Navigation was performed from another task of the same framework lessons
      if (fromTask != null && fromTask.lesson == lesson) {
        executeInitScripts(project, listOf(task))
      }
    }
  }
}
