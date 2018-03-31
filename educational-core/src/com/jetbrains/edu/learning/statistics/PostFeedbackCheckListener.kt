package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.ProgressUtil


class PostFeedbackCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (isFeedbackAsked()) {
      return
    }

    val lesson = task.lesson
    val course = lesson.course

    val progress = ProgressUtil.countProgressWithoutSubtasks(course)
    val solvedTasks = progress.first
    if (solvedTasks == lesson.taskList.size) {
      showNotification(true, course, project)
    }
  }
}
