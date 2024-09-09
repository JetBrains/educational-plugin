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
import com.jetbrains.edu.learning.notification.EduNotificationManager

class LTICheckListener : CheckListener {

  override fun afterCheck(
    project: Project,
    task: Task,
    result: CheckResult
  ) {
    val course = task.lesson.course as? EduCourse ?: return
    if (!course.isStudy) return

    val ltiSettings = LTISettingsManager.instance(project).state
    val launchId = ltiSettings.launchId ?: return

    logger<LTICheckListener>().info("Posting check result for task ${task.name}. solved=${result.isSolved} launchId=$launchId")

    if (result.isSolved) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val error = LTIConnector.getInstance().postTaskSolved(launchId, task.course.id, task.id)

        runInEdt {
          notifyPostingStatus(error, ltiSettings.lmsDescription, project)
        }
      }
    }
  }

  private fun notifyPostingStatus(error: String?, lmsDescription: String?, project: Project) {
    //TODO bundle the following texts
    val lmsName = if (lmsDescription.isNullOrEmpty()) {
      "LMS"
    }
    else {
      "LMS: $lmsDescription"
    }

    if (error != null) {
      EduNotificationManager.create(
        ERROR,
        "Failed to post to LMS",
        "Failed to post your achievement to $lmsName. Error message: $error"
      ).notify(project)
    }
    else {
      EduNotificationManager.create(
        NotificationType.INFORMATION,
        "Achievement successfully posted",
        "Your achievement was successfully posted to $lmsName",
      ).notify(project)
    }
  }
}
