package com.jetbrains.edu.learning.checkio.notifications.errors

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotifications
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.event.HyperlinkEvent

class CheckiOErrorReporter(
  private val project: Project,
  @Suppress("UnstableApiUsage")
  private val title: @NlsContexts.NotificationTitle String,
  private val oAuthConnector: CheckiOOAuthConnector
) {
  private fun reportLoginRequiredError() {
    CheckiONotifications.showError(
      project,
      title,
      "",
      EduCoreBundle.message("notification.content.log.in.and.try.again.checkio"),
      LoginLinkListener(oAuthConnector)
    )
  }

  private fun reportNetworkError() {
    CheckiONotifications.showWarning(
      project,
      title,
      EduCoreBundle.message("notification.subtitle.connection.failed"),
      EduCoreBundle.message("notification.content.check.connection.and.try.again"),
    )
  }

  private fun reportUnexpectedError() {
    CheckiONotifications.showError(
      project,
      title,
      "",
      EduCoreBundle.message("notification.content.unexpected.error.occurred")
    )
  }

  fun handle(e: Exception) {
    LOG.warn(e)
    when (e) {
      is CheckiOLoginRequiredException -> reportLoginRequiredError()
      is NetworkException -> reportNetworkError()
      else -> reportUnexpectedError()
    }
  }

  companion object {
    private val LOG = logger<CheckiOErrorReporter>()
  }

  private class LoginLinkListener(private val connector: CheckiOOAuthConnector) : NotificationListener.Adapter() {
    override fun hyperlinkActivated(notification: Notification, event: HyperlinkEvent) {
      connector.doAuthorize(Runnable { ApplicationManager.getApplication().invokeLater { notification.hideBalloon() } })
    }
  }
}
