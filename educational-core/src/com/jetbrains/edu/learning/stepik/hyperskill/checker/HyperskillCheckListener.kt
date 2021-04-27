package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLoginListener
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillNotificationGroup
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course as? HyperskillCourse ?: return
    if (!course.isStudy) {
      return
    }

    if (course.isTaskInProject(task)) {
      if (HyperskillSettings.INSTANCE.account == null) {
        Notification(
          hyperskillNotificationGroup.displayId,
          EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA),
          EduCoreBundle.message("error.login.required", EduNames.JBA),
          NotificationType.ERROR
        ) { notification, e ->
          notification.expire()
          HyperskillLoginListener.hyperlinkUpdate(e)
        }.notify(project)
        return
      }

      if (!isUnitTestMode) {
        ApplicationManager.getApplication().executeOnPooledThread {
          HyperskillCheckConnector.postStageSolution(task, project, result)
        }
      }
      else {
        HyperskillCheckConnector.postStageSolution(task, project, result)
      }
    }
  }
}