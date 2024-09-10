package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager

class LTICheckListener : CheckListener {

  override fun afterCheck(
    project: Project,
    task: Task,
    result: CheckResult
  ) {
    val course = task.lesson.course as? EduCourse ?: return
    if (!result.isSolved) return // currently, we report only successful solutions

    if (!course.isStudy || !course.isMarketplaceRemote) return

    val ltiSettings = LTISettingsManager.instance(project).state
    val launchId = ltiSettings.launchId ?: return

    logger<LTICheckListener>().info("Posting check result for task ${task.name}: solved=${result.isSolved}, launchId=$launchId")

    ApplicationManager.getApplication().executeOnPooledThread {
      val error = LTIConnector.getInstance().postTaskSolved(launchId, task.course.id, task.id)

      runInEdt {
        notifyPostingStatus(error, ltiSettings.lmsDescription, project)
      }
    }
  }

  private fun notifyPostingStatus(error: String?, lmsDescription: String?, project: Project) {
    if (error != null) {
      EduNotificationManager.create(
        ERROR,
        EduCoreBundle.message("lti.grades.post.error.title"),
        if (lmsDescription.isNullOrEmpty()) {
          EduCoreBundle.message("lti.grades.post.error.text", error)
        }
        else {
          EduCoreBundle.message("lti.grades.post.error.text.with.lms", lmsDescription, error)
        }
      ).notify(project)
    }
    else {
      EduNotificationManager.create(
        NotificationType.INFORMATION,
        EduCoreBundle.message("lti.grades.post.success.title"),
        if (lmsDescription.isNullOrEmpty()) {
          EduCoreBundle.message("lti.grades.post.success.text")
        }
        else {
          EduCoreBundle.message("lti.grades.post.success.text.with.lms", lmsDescription)
        }
      ).notify(project)
    }
  }
}
