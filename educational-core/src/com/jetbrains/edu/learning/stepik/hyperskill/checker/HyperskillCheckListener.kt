package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.ProgressUtil
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_URL
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course as? HyperskillCourse ?: return

    if (course.isTaskInProject(task)) {
      if (HyperskillSettings.INSTANCE.account == null) {
        val notification = Notification("hyperskill",
                                        "Failed to post solution to ${EduNames.JBA}",
                                        "Please, log in to post solutions to ${EduNames.JBA}",
                                        NotificationType.WARNING)
        notification.notify(project)
        return
      }
      ApplicationManager.getApplication().executeOnPooledThread {
        HyperskillCheckConnector.postSolution(task, project, result)
      }
      showChooseNewProjectNotification(course, project)
    }
  }

  private fun showChooseNewProjectNotification(course: HyperskillCourse, project: Project) {
    val lesson = course.getProjectLesson() ?: return
    val (solved, total) = ProgressUtil.countProgress(lesson)
    if (solved == total) {
      val notification = Notification(HYPERSKILL,
                                      "Well done!", "Congratulations! You finished this project. " +
                                                    "Visit <a href=\"$HYPERSKILL_PROJECTS_URL\">${EduNames.JBA}</a> to choose new project.",
                                      NotificationType.INFORMATION, NotificationListener.URL_OPENING_LISTENER)
      notification.notify(project)
    }
  }
}