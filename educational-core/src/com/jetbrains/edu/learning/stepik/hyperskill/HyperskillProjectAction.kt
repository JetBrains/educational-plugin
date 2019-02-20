package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.labels.ActionLink
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.event.HyperlinkEvent

class HyperskillProjectAction : DumbAwareAction("Start Hyperskill Project") {

  override fun actionPerformed(e: AnActionEvent) {
    val account = HyperskillSettings.INSTANCE.account
    if (account == null) {
      showBalloon(e, "Please, <a href=\"\">login to Hyperskill</a> and select project.", true)
    }
    else {
      val hyperskillProject = ProgressManager.getInstance().run(
        object : Task.WithResult<HyperskillProject?, Exception>(null, "Loading Selected Project", false) {
          override fun compute(indicator: ProgressIndicator): HyperskillProject? {
            val currentUser = HyperskillConnector.getCurrentUser(account)
            if (currentUser != null) {
              account.userInfo = currentUser
            }
            val projectId = account.userInfo.hyperskillProjectId ?: return null
            return HyperskillConnector.getProject(projectId)
          }
        })
      if (hyperskillProject == null) {
        showBalloon(e, "Please, <a href=\"$HYPERSKILL_PROJECTS_URL\">select project</a> ", false)
      }
      else if (!hyperskillProject.useIde) {
        showBalloon(e, HYPERSKILL_PROJECT_NOT_SUPPORTED, false)
      }
      else {
        val languageId = EduNames.JAVA
        val hyperskillCourse = HyperskillCourse(hyperskillProject, languageId)
        if (hyperskillCourse.configurator == null) {
          showBalloon(e, HYPERSKILL_PROJECT_NOT_SUPPORTED, false)
        } else {
          HyperskillJoinCourseDialog(hyperskillCourse).show()
        }
      }
    }
  }

  private fun showBalloon(e: AnActionEvent, message: String, authorize: Boolean) {
    val builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
    builder.setHideOnClickOutside(true)
    builder.setClickHandler(HSHyperlinkListener(authorize), true)
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
}

class HSHyperlinkListener(private val authorize: Boolean) : ActionListener, NotificationListener {
  override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
    authorizeOrBrowse()
  }

  override fun actionPerformed(e: ActionEvent?) {
    authorizeOrBrowse()
  }

  private fun authorizeOrBrowse() {
    if (authorize) {
      HyperskillConnector.doAuthorize()
    }
    else {
      BrowserUtil.browse(HYPERSKILL_PROJECTS_URL)
    }
  }
}
