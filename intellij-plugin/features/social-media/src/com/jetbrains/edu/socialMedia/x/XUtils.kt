package com.jetbrains.edu.socialMedia.x

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import java.nio.file.Path

object XUtils {

  const val PLATFORM_NAME: String = "X"

  private val LOG = logger<XUtils>()

  @RequiresBackgroundThread
  fun doPost(project: Project, message: String, imagePath: Path?) {
    try {
      val connector = XConnector.getInstance()
      if (connector.account == null) {
        connector.doAuthorize({
          runInEdt {
            requestFocus()
          }
          tweet(project, message, imagePath)
        })
      }
      else {
        tweet(project, message, imagePath)
      }
    }
    catch (e: Exception) {
      LOG.warn(e)
      showFailedToPostNotification(project)
    }
  }

  private fun tweet(project: Project, message: String, imagePath: Path?) {
    val response = try {
      XConnector.getInstance().tweet(message, imagePath)
    }
    catch (e: Exception) {
      LOG.warn(e)
      null
    }

    if (response?.data?.id != null) {
      showSuccessNotification(project, response.data.id)
    }
    else {
      showFailedToPostNotification(project)
    }
  }

  private fun showFailedToPostNotification(project: Project) {
    EduNotificationManager.showErrorNotification(
      project,
      EduSocialMediaBundle.message("social.media.error.failed.to.post.notification"),
      EduSocialMediaBundle.message("social.media.error.failed.to.post.notification")
    )
  }

  private fun showSuccessNotification(project: Project, postId: String) {
    EduNotificationManager
      .create(INFORMATION, EduSocialMediaBundle.message("social.media.success.notification.title"), EduSocialMediaBundle.message("x.tweet.posted"))
      .addAction(NotificationAction.createSimple(EduSocialMediaBundle.message("social.media.open.in.browser.notification.action.text")) {
        EduBrowser.getInstance().browse("https://x.com/anyuser/status/${postId}")
      })
      .notify(project)
  }
}
