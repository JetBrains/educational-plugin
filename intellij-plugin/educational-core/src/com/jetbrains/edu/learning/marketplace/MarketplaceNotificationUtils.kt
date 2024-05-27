package com.jetbrains.edu.learning.marketplace

import com.intellij.icons.ExpUiIcons
import com.intellij.ide.plugins.PluginManagerConfigurable
import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FAILED_TO_DELETE_SUBMISSIONS
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduErrorNotification
import com.jetbrains.edu.learning.notification.EduInformationNotification
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NotNull

object MarketplaceNotificationUtils {
  fun showReloginToJBANeededNotification(action: AnAction) {
    val notification = EduErrorNotification(
      EduCoreBundle.message("jba.relogin.needed.title"),
      EduCoreBundle.message("jba.relogin.text"),
    )
    notification.addAction(action)
    notification.notify(null)
  }

  fun showInstallMarketplacePluginNotification() {
    val notification = EduErrorNotification(
      EduCoreBundle.message("error.failed.login.to.subsystem", MARKETPLACE),
      EduCoreBundle.message("notification.marketplace.install.licensing.plugin"),
    )

    notification.addAction(object : AnAction(EduCoreBundle.message("action.install.plugin.in.settings")) {
      override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
          ProjectManager.getInstance().defaultProject, PluginManagerConfigurable::class.java
        )
      }
    })
    notification.notify(null)
  }

  fun showLoginNeededNotification(
    project: Project?,
    failedActionTitle: String,
    notificationTitle: String = EduCoreBundle.message("notification.title.authorization.required"),
    authAction: () -> Unit
  ) {
    val notification = EduErrorNotification(
      notificationTitle,
      EduCoreBundle.message("notification.content.authorization", failedActionTitle),
    )

    @Suppress("DialogTitleCapitalization")
    notification.addAction(object : DumbAwareAction(EduCoreBundle.message("notification.content.authorization.action")) {
      override fun actionPerformed(e: AnActionEvent) {
        authAction()
        notification.expire()
      }
    })

    notification.notify(project)
  }

  fun showLoginToUseSubmissionsNotification(project: Project) {
    val notification = EduInformationNotification(
      content = EduCoreBundle.message("submissions.tab.login", JET_BRAINS_ACCOUNT),
    )
    notification.addAction(object : AnAction(EduCoreBundle.message("log.in.to", JET_BRAINS_ACCOUNT)) {
      override fun actionPerformed(e: AnActionEvent) {
        MarketplaceConnector.getInstance().doAuthorize(Runnable {
          SubmissionsManager.getInstance(project).prepareSubmissionsContentWhenLoggedIn {
            MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground()
          }
        })
        notification.notify(project)
      }
    })
  }

  fun showFailedToFindMarketplaceCourseOnRemoteNotification(project: Project, action: AnAction) {
    CCNotificationUtils.showErrorNotification(
      project,
      EduCoreBundle.message("error.failed.to.update"),
      EduCoreBundle.message("marketplace.failed.to.update.no.course"),
      action
    )
  }

  fun showMarketplaceAccountNotification(project: Project, message: String, action: () -> AnAction) {
    CCNotificationUtils.showErrorNotification(
      project,
      EduCoreBundle.message("notification.course.creator.failed.to.upload.course.title"),
      message,
      action()
    )
  }

  fun showNoRightsToUpdateNotification(project: Project, course: EduCourse, action: () -> Unit) {
    CCNotificationUtils.showErrorNotification(project,
      EduCoreBundle.message("notification.course.creator.access.denied.title"),
      EduCoreBundle.message("notification.course.creator.access.denied.content"),
      NotificationAction.createSimpleExpiring(
        EduCoreBundle.message("notification.course.creator.access.denied.action")
      ) {
        course.convertToLocal()
        action()
      })
  }

  fun showFailedToPushCourseNotification(project: Project, courseName: String) {
    EduNotificationManager.showErrorNotification(
      project,
      EduCoreBundle.message("marketplace.push.course.failed.title"),
      EduCoreBundle.message("marketplace.push.course.failed.text", courseName)
    )
  }

  fun showFailedToChangeSharingPreferenceNotification() {
    EduNotificationManager.showErrorNotification(
      title = EduCoreBundle.message("marketplace.solutions.sharing.notification.title"),
      content = EduCoreBundle.message("notification.something.went.wrong.text")
    )
  }

  internal fun showSubmissionsDeletedSuccessfullyNotification(project: Project?, courseId: Int?, loginName: String?) {
    val presentableName = project?.course?.presentableName
    val message = if (loginName != null && courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.on.course.success.message", loginName, presentableName)
    }
    else if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.success.message", loginName)
    }
    else if (courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.success.on.course.message", presentableName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.success.message")
    }

    EduNotificationManager.showInfoNotification(
      project,
      EduCoreBundle.message("marketplace.delete.submissions.success.title"),
      message,
    )
  }

  internal fun showNoSubmissionsToDeleteNotification(project: Project?, courseId: Int?, loginName: String?) {
    val presentableName = project?.course?.presentableName
    val message = if (loginName != null && courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.on.course.nothing.message", loginName, presentableName)
    }
    else if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.nothing.message", loginName)
    }
    else if (courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.on.course.nothing.message", presentableName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.nothing.message")
    }

    EduNotificationManager.showInfoNotification(
      project,
      EduCoreBundle.message("marketplace.delete.submissions.nothing.title"),
      message,
    )
  }

  internal fun showFailedToDeleteNotification(project: Project?, courseId: Int?, loginName: String?) {
    val presentableName = project?.course?.presentableName
    val message = if (loginName != null && courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.failed.for.user.on.course.message", loginName, presentableName)
    }
    else if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.failed.for.user.message", loginName)
    }
    else if (courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.failed.on.course.message", presentableName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.failed.message")
    }

    EduNotificationManager.showErrorNotification(
      project,
      EduCoreBundle.message("marketplace.delete.submissions.failed.title"),
      message
    ) {
      addAction(
        BrowseNotificationAction(
          EduCoreBundle.message("marketplace.delete.submissions.failed.troubleshooting.text"),
          FAILED_TO_DELETE_SUBMISSIONS
        )
      )
    }
  }

  fun showSuccessRequestNotification(
    project: Project?,
    @NotNull @NlsContexts.NotificationTitle title: String,
    @NotNull @NlsContexts.NotificationContent message: String
  ) {
    EduInformationNotification(title, message)
      .apply {
        // workaround: there is no NotificationType.Success in the platform yet
        icon = ExpUiIcons.Status.Success
      }.notify(project)
  }
}