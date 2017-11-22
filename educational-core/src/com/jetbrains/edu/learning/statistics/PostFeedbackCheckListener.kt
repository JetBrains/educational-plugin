package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow

class PostFeedbackCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task) {
    if (isFeedbackAsked()) {
      return
    }

    val lesson = task.lesson
    val course = lesson.course
    val lessons = course.lessons

    val progress = TaskDescriptionToolWindow.countProgressWithoutSubtasks(lessons)
    val solvedTasks = progress.getFirst()
    if (solvedTasks == lesson.taskList.size) {
      showNotification(true, course, project)
    }
  }
}
