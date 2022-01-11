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
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.Companion.isRemotelyChecked
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (task.isRemotelyChecked()) {
      /**
       * Solution must be sent after local tests check are made for Edu tasks.
       * Opposite to Edu tasks, e.g., there are no local tests check for Code tasks and code is submitted directly to JBA.
       */
      return
    }

    val course = task.lesson.course as? HyperskillCourse ?: return
    if (!course.isStudy) {
      return
    }

    if (HyperskillSettings.INSTANCE.account == null) {
      Notification(
        "EduTools",
        EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA),
        EduCoreBundle.message("error.login.required", EduNames.JBA),
        NotificationType.ERROR
      ).setListener { notification, e ->
        notification.expire()
        HyperskillLoginListener.hyperlinkUpdate(e)
      }.notify(project)
      return
    }

    if (!isUnitTestMode) {
      ApplicationManager.getApplication().executeOnPooledThread {
        HyperskillCheckConnector.postEduTaskSolution(task, project, result)
      }
    }
    else {
      HyperskillCheckConnector.postEduTaskSolution(task, project, result)
    }
  }
}