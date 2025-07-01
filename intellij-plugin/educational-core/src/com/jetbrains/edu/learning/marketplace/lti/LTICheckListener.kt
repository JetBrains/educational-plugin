package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LTICheckListener : CheckListener {

  override fun afterCheck(
    project: Project,
    task: Task,
    result: CheckResult
  ) {
    val course = task.lesson.course as? EduCourse ?: return

    if (!course.isStudy || !course.isMarketplaceRemote) return

    val ltiSettings = LTISettingsManager.getInstance(project).settings ?: return

    val solved = result.isSolved
    if (!solved && ltiSettings.onlineService == LTIOnlineService.ALPHA_TEST_2024) return

    val launchId = ltiSettings.launchId

    logger<LTICheckListener>().info("Posting completion status for task ${task.name}: solved=$solved, launchId=$launchId")

    val response = runWithModalProgressBlocking(project, EduCoreBundle.message("lti.posting.completion.status")) {
      withContext(Dispatchers.IO) {
        LTIConnector.getInstance().postTaskChecked(ltiSettings.onlineService, launchId, task.course.id, task.id, solved)
      }
    }

    notifyPostingResponse(response, ltiSettings.lmsDescription, project, launchId)
  }

  private fun notifyPostingResponse(response: PostTaskSolvedStatus, lmsDescription: String?, project: Project, launchId: String) {
    when (response) {
      is ConnectionError -> errorNotification(project, lmsDescription, response.error, launchId)
      ServerError -> errorNotification(project, lmsDescription, "server error", launchId)
      UnknownLaunchId -> errorNotification(project, lmsDescription, "unknown launch id", launchId)

      NoLineItem -> {} //do nothing
      Success -> {
        EduNotificationManager.create(
          NotificationType.INFORMATION,
          EduCoreBundle.message("lti.grades.post.status.sent.title"),
          if (lmsDescription.isNullOrEmpty()) {
            EduCoreBundle.message("lti.grades.post.status.sent.text")
          }
          else {
            EduCoreBundle.message("lti.grades.post.status.sent.text.with.lms", lmsDescription)
          }
        ).notify(project)
      }
    }
  }

  private fun errorNotification(project: Project, lmsDescription: String?, error: String, launchId: String) {
    logger<LTICheckListener>().warn("error during posting completion status launchId=$launchId, error=$error")

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
}
