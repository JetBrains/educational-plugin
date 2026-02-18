package com.jetbrains.edu.lti

import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
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

    val response = runWithModalProgressBlocking(project, LTIBundle.message("posting.completion.status")) {
      withContext(Dispatchers.IO) {
        LTIConnector.getInstance().postTaskChecked(ltiSettings.onlineService, launchId, task.course.id, task.id, solved)
      }
    }

    notifyPostingResponse(response, ltiSettings, project, launchId)
  }

  private fun notifyPostingResponse(response: PostTaskSolvedStatus, ltiSettings: LTISettingsDTO, project: Project, launchId: String) {

    when (response) {
      is ConnectionError -> errorNotification(project, ltiSettings, response.error, launchId)

      UnknownLaunchId -> errorNotification(project, ltiSettings, "unknown launch id", launchId)

      ServerError -> errorNotification(project, ltiSettings, null, launchId, suggestOpeningCourseOnline = true)

      NoLineItem -> {} //do nothing
      Success -> {
        val lmsDescription = ltiSettings.lmsDescription

        EduNotificationManager.create(
          NotificationType.INFORMATION,
          LTIBundle.message("grades.post.status.sent.title"),
          if (lmsDescription.isNullOrEmpty()) {
            LTIBundle.message("grades.post.status.sent.text")
          }
          else {
            LTIBundle.message("grades.post.status.sent.text.with.lms", lmsDescription)
          }
        ).notify(project)
      }
    }
  }

  private fun errorNotification(
    project: Project,
    ltiSettings: LTISettingsDTO,
    error: String?,
    launchId: String,
    suggestOpeningCourseOnline: Boolean = false
  ) {
    logger<LTICheckListener>().warn("error during posting completion status launchId=$launchId, error=$error")

    val errorMessage = buildString {
      append(LTIBundle.message("grades.post.error.text"))

      if (error != null) {
        append("<br>")
        append(LTIBundle.message("grades.post.error.text.error", error))
      }

      if (suggestOpeningCourseOnline) {
        val platformName = ApplicationNamesInfo.getInstance().fullProductName
        append("<br>")
        append(LTIBundle.message("grades.post.error.text.try.reopen", platformName))
      }

      val lmsDescription = ltiSettings.lmsDescription
      val linkToCourse = ltiSettings.returnLink
      val linkText = if (lmsDescription.isNullOrEmpty()) { linkToCourse } else { lmsDescription }
      val yourCourseText = if (linkToCourse.isNullOrEmpty()) linkText else """<a href="$linkToCourse">$linkText</a>"""
      if (yourCourseText != null) {
        append("<br>")
        append(LTIBundle.message("grades.post.error.text.link") + " " + yourCourseText)
      }
    }

    EduNotificationManager.create(
      ERROR,
      LTIBundle.message("grades.post.error.title"),
      errorMessage
    ).notify(project)
  }
}
