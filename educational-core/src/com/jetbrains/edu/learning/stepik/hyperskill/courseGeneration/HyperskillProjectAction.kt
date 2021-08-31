package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.components.labels.ActionLink
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.SELECT_PROJECT
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.getSelectedProjectIdUnderProgress
import com.jetbrains.edu.learning.stepik.hyperskill.isHyperskillSupportAvailable
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import java.net.URL
import javax.swing.event.HyperlinkEvent

class HyperskillProjectAction : DumbAwareAction("Open ${EduNames.JBA} Project") {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isHyperskillSupportAvailable()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val account = HyperskillSettings.INSTANCE.account
    val accessToken = account?.getAccessToken()
    if (account == null || HyperskillConnector.getInstance().getCurrentUser(account, accessToken) == null) {
      showBalloon(e, "Please <a href=\"\">login to ${EduNames.JBA}</a> and select a project.", true)
    }
    else {
      openHyperskillProject(account) { error -> showBalloon(e, error, false) }
    }
  }

  private fun showBalloon(e: AnActionEvent, message: String, authorize: Boolean) {
    val builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.INFO, HSHyperlinkListener(authorize))
    builder.setHideOnClickOutside(true)
    val balloon = builder.createBalloon()

    val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    if (component is ActionLink) {
      balloon.showInCenterOf(component)
    }
    else {
      val relativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(e.dataContext)
      balloon.show(relativePoint, Balloon.Position.above)
    }
  }

  companion object {
    fun openHyperskillProject(account: HyperskillAccount, showError: (String) -> Unit): Boolean {
      val projectId = getSelectedProjectIdUnderProgress(account)
      if (projectId == null) {
        showError(SELECT_PROJECT)
        return false
      }

      return ProjectOpener.getInstance()
        .open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStageRequest(projectId, null)).onError {
          showError(it)
          return false
        }
    }
  }
}

class HSHyperlinkListener(private val authorize: Boolean) : NotificationListener, HyperlinkAdapter() {
  override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
    authorizeOrBrowse(event.url)
  }

  private fun authorizeOrBrowse(url: URL?) {
    if (authorize) {
      HyperskillConnector.getInstance().doAuthorize()
      EduCounterUsageCollector.loggedIn(HYPERSKILL, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
    }
    else {
      if (url != null) {
        EduBrowser.getInstance().browse(url)
      }
    }
  }

  override fun hyperlinkActivated(e: HyperlinkEvent?) {
    authorizeOrBrowse(e?.url)
  }
}
