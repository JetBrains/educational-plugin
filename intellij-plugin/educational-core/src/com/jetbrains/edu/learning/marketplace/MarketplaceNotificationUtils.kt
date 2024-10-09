package com.jetbrains.edu.learning.marketplace

import com.intellij.ide.plugins.PluginManagerConfigurable
import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.INFORMATION
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
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NotNull

object MarketplaceNotificationUtils {
  fun showReloginToJBANeededNotification(action: AnAction) {
    EduNotificationManager
      .create(ERROR, EduCoreBundle.message("jba.relogin.needed.title"), EduCoreBundle.message("jba.relogin.text"))
      .addAction(action)
      .notify(null)
  }

  fun showInstallMarketplacePluginNotification() {
    EduNotificationManager.create(
      ERROR,
      EduCoreBundle.message("error.failed.login.to.subsystem", MARKETPLACE),
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("notification.marketplace.install.licensing.plugin")
    ).addAction(object : AnAction(EduCoreBundle.message("action.install.plugin.in.settings")) {
      override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
          ProjectManager.getInstance().defaultProject, PluginManagerConfigurable::class.java
        )
      }
    }).notify(null)
  }

  fun showLoginNeededNotification(
    project: Project?,
    failedActionTitle: String,
    notificationTitle: String = EduCoreBundle.message("notification.title.authorization.required"),
    authAction: () -> Unit
  ) {
    EduNotificationManager
      .create(ERROR, notificationTitle, EduCoreBundle.message("notification.content.authorization", failedActionTitle))
      .apply {
        @Suppress("DialogTitleCapitalization")
        addAction(object : DumbAwareAction(EduCoreBundle.message("notification.content.authorization.action")) {
          override fun actionPerformed(e: AnActionEvent) {
            authAction()
            this@apply.expire()
          }
        })
      }.notify(project)
  }

  fun showLoginToUseSubmissionsNotification(project: Project) {
    EduNotificationManager
      .create(INFORMATION, content = EduCoreBundle.message("submissions.tab.login", JET_BRAINS_ACCOUNT))
      .apply {
        addAction(object : AnAction(EduCoreBundle.message("log.in.to", JET_BRAINS_ACCOUNT)) {
          override fun actionPerformed(e: AnActionEvent) {
            MarketplaceConnector.getInstance().doAuthorize(Runnable {
              SubmissionsManager.getInstance(project).prepareSubmissionsContentWhenLoggedIn {
                MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground()
              }
            })
            this@apply.notify(project)
          }
        })
      }.notify(project)
  }

  fun showFailedToFindMarketplaceCourseOnRemoteNotification(project: Project, action: AnAction) {
    CCNotificationUtils.showErrorNotification(
      project,
      EduCoreBundle.message("error.failed.to.update"),
      EduCoreBundle.message("marketplace.failed.to.update.no.course"),
      action
    )
  }

  fun showFailedToChangeSharingPreferenceNotification() {
    EduNotificationManager.showErrorNotification(
      title = @Suppress("DialogTitleCapitalization") EduCoreBundle.message("marketplace.solutions.sharing.notification.title"),
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
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("marketplace.delete.submissions.success.title"),
      message
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
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("marketplace.delete.submissions.nothing.title"),
      message
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

    EduNotificationManager.create(
      ERROR,
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("marketplace.delete.submissions.failed.title"),
      message
    ).addAction(
      BrowseNotificationAction(
        EduCoreBundle.message("marketplace.delete.submissions.failed.troubleshooting.text"), FAILED_TO_DELETE_SUBMISSIONS
      )
    ).notify(project)
  }

  fun showSuccessRequestNotification(
    project: Project?,
    @NotNull @NlsContexts.NotificationTitle title: String,
    @NotNull @NlsContexts.NotificationContent message: String
  ) {
    EduNotificationManager
      .create(INFORMATION, title, message)
      // workaround: there is no NotificationType.Success in the platform yet
      .setIcon(getSuccessIcon())
      .notify(project)
  }

  fun showSubmissionNotPostedNotification(project: Project, course: EduCourse, taskName: String) {
    EduNotificationManager.create(INFORMATION,
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("error.solution.not.posted"),
      EduCoreBundle.message("notification.content.task.was.updated", taskName),
    ).addAction(NotificationAction.create { course.checkForUpdates(project, true) {} }).notify(project)
  }
}