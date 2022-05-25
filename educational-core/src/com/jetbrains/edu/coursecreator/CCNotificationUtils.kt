package com.jetbrains.edu.coursecreator

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.event.HyperlinkEvent

@Suppress("UnstableApiUsage")
object CCNotificationUtils {
  private val LOG = Logger.getInstance(CCNotificationUtils::class.java)

  @JvmStatic
  val showLogAction: AnAction
    get() = ActionManager.getInstance().getAction("ShowLog")

  @JvmStatic
  fun showFailedToPostItemNotification(project: Project, item: StudyItem, isNew: Boolean) {
    val pathInCourse = item.getPathInCourse()

    val title = if (isNew) EduCoreBundle.message("notification.course.creator.failed.to.upload.item.title")
    else EduCoreBundle.message("notification.course.creator.failed.to.update.item.title")

    val content = if (isNew) EduCoreBundle.message("notification.course.creator.failed.to.upload.item.content", pathInCourse)
    else EduCoreBundle.message("notification.course.creator.failed.to.update.item.content", pathInCourse)

    showErrorNotification(project, title, content, showLogAction)
  }

  @JvmStatic
  @JvmOverloads
  fun showErrorNotification(project: Project,
                            @NotificationTitle title: String,
                            @NotificationContent message: String? = null,
                            action: AnAction? = null
  ) {
    LOG.info(message)
    val notification = Notification("EduTools", title, message.orEmpty(), NotificationType.ERROR)
    if (action != null) {
      notification.addAction(action)
    }
    notification.notify(project)
  }

  @JvmStatic
  fun showNoRightsToUpdateOnStepikNotification(project: Project) {
    showErrorNotification(project,
                          EduCoreBundle.message("notification.course.creator.access.denied.title"),
                          EduCoreBundle.message("notification.course.creator.access.denied.content"))
  }

  fun showNoRightsToUpdateNotification(project: Project, course: EduCourse, action: () -> Unit) {
    showErrorNotification(project,
                          EduCoreBundle.message("notification.course.creator.access.denied.title"),
                          EduCoreBundle.message("notification.course.creator.access.denied.content"),
                          NotificationAction.createSimpleExpiring(
                            EduCoreBundle.message("notification.course.creator.access.denied.action")) {
                            course.convertToLocal()
                            action()
                          })
  }

  fun showAcceptDeveloperAgreementNotification(project: Project, action: () -> AnAction) {
    showErrorNotification(project,
                          EduCoreBundle.message("notification.course.creator.failed.to.upload.course.title"),
                          EduCoreBundle.message("marketplace.plugin.development.agreement.not.accepted"),
                          action()
    )
  }

  fun showFailedToFindMarketplaceCourseOnRemoteNotification(project: Project, action: AnAction) {
    showErrorNotification(project,
                          EduCoreBundle.message("error.failed.to.update"),
                          EduCoreBundle.message("marketplace.failed.to.update.no.course"),
                          action)
  }

  fun createPostCourseNotificationListener(course: EduCourse, action: () -> Unit): NotificationListener.Adapter {
    return object : NotificationListener.Adapter() {
      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        notification.expire()
        course.convertToLocal()
        action()
      }
    }
  }

  @JvmStatic
  fun showNotification(project: Project,
                       @NotificationContent title: String,
                       action: AnAction?) {
  showNotification(project, action, title, "")
  }

  fun showNotification(project: Project,
                       action: AnAction?,
                       @NotificationTitle title: String,
                       @NotificationContent message: String,
                       notificationType: NotificationType = NotificationType.INFORMATION) {
    val notification = Notification("EduTools", title, message, notificationType)
    if (action != null) {
      notification.addAction(action)
    }
    notification.notify(project)
  }

  fun showLoginSuccessfulNotification(userName: String) {
    Notification(
      "EduTools",
      EduCoreBundle.message("login.successful"),
      EduCoreBundle.message("logged.in.as", userName),
      NotificationType.INFORMATION
    ).notify(null)
  }
}
