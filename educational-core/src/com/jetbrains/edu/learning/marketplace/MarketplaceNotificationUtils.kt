package com.jetbrains.edu.learning.marketplace

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle

object MarketplaceNotificationUtils {
  fun showLoginFailedNotification(loginProviderName: String) {
    Notification(
      "EduTools",
      EduCoreBundle.message("error.login.failed"),
      EduCoreBundle.message("error.failed.login.to.subsystem", loginProviderName),
      NotificationType.ERROR
    ).notify(null)
  }

  fun showReloginToJBANeededNotification() {
    Notification(
      "EduTools",
      EduCoreBundle.message("jba.relogin.needed.title"),
      EduCoreBundle.message("jba.relogin.text"),
      NotificationType.ERROR
    ).notify(null)
  }

  fun showFailedToFindMarketplaceCourseOnRemoteNotification(project: Project, action: AnAction) {
    CCNotificationUtils.showErrorNotification(project,
                                              EduCoreBundle.message("error.failed.to.update"),
                                              EduCoreBundle.message("marketplace.failed.to.update.no.course"),
                                              action)
  }


  fun showAcceptDeveloperAgreementNotification(project: Project, action: () -> AnAction) {
    CCNotificationUtils.showErrorNotification(project,
                                              EduCoreBundle.message("notification.course.creator.failed.to.upload.course.title"),
                                              EduCoreBundle.message("marketplace.plugin.development.agreement.not.accepted"),
                                              action()
    )
  }

  fun showNoRightsToUpdateNotification(project: Project, course: EduCourse, action: () -> Unit) {
    CCNotificationUtils.showErrorNotification(project,
                                              EduCoreBundle.message("notification.course.creator.access.denied.title"),
                                              EduCoreBundle.message("notification.course.creator.access.denied.content"),
                                              NotificationAction.createSimpleExpiring(
                                                EduCoreBundle.message("notification.course.creator.access.denied.action")) {
                                                course.convertToLocal()
                                                action()
                                              })
  }
}