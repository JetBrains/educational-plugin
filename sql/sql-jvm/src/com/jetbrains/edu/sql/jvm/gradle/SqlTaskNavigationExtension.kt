package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.navigation.TaskNavigationExtension

class SqlTaskNavigationExtension : TaskNavigationExtension {
  override fun onTaskNavigation(project: Project, task: Task, fromTask: Task?) {
    val lesson = task.lesson
    if (lesson is FrameworkLesson && lesson.course.isStudy) {
      for (file in FileEditorManager.getInstance(project).openFiles) {
        if (file.getTaskFile(project)?.task == task) {
          attachSqlConsoleIfNeeded(project, file)
        }
      }
    }
  }
}
