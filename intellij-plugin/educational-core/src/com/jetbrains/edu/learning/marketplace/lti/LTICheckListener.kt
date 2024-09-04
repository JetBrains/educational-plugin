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

    val ltiSettingsManager = LTISettingsManager.instance(project)
    // This call reloads data from file.
    // Later, we will remove this line because we don't expect users to modify the LTI yaml file manually.
    // Reloading will be done on project opening and when the project is launched from the browser.
    // As a downside, now this file is reloaded each time a learner checks a task.
    ltiSettingsManager.reloadSettingsFromFile()
    val ltiLaunch = ltiSettingsManager.ltiSettings?.launches?.firstOrNull() ?: return

    logger<LTICheckListener>().info("Posting check result for task ${task.name}. solved=${result.isSolved} launchId=$ltiLaunch")

    if (result.isSolved) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val error = LTIConnector.getInstance().postTaskSolved(ltiLaunch.id, task.course.id, task.id)

        runInEdt {
          notifyPostingStatus(error, ltiLaunch, project)
        }
      }
    }
  }

  private fun notifyPostingStatus(error: String?, ltiLaunch: LTILaunch, project: Project) {
    //TODO bundle the following texts
    val lmsName = if (ltiLaunch.lmsDescription != "") {
      "LMS: ${ltiLaunch.lmsDescription}"
    }
    else {
      "LMS"
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
