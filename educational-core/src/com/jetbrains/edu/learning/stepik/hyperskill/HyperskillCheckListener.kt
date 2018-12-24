package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.ProgressUtil
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course
    if (course !is HyperskillCourse || HyperskillSettings.INSTANCE.account == null) return

    HyperskillConnector.postSolution(task, project)

    showChooseNewProjectNotification(course, project)
  }

  private fun showChooseNewProjectNotification(course: Course, project: Project) {
    val (solved, total) = ProgressUtil.countProgress(course)
    if (solved == total) {
      val notification = Notification("hyperskill",
                                      "Well done!", "Congratulations! You finished this project. " +
                                                    "Visit <a href=\"$HYPERSKILL_URL\">Hyperskill</a> to choose new project.",
                                      NotificationType.INFORMATION)
      notification.notify(project)
    }
  }
}