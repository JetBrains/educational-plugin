package com.jetbrains.edu.coursecreator.actions.marketplace

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.coursecreator.CCUtils.checkIfAuthorized
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.JB_VENDOR_NAME
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.io.File

@Suppress("ComponentNotRegistered") // Marketplace.xml
class MarketplacePushCourse(private val updateTitle: String = message("item.update.on.0.course.title", MARKETPLACE),
                            private val uploadTitle: String = message("item.upload.to.0.course.title", MARKETPLACE)) : DumbAwareAction(
  EduCoreBundle.lazyMessage("gluing.slash", updateTitle, uploadTitle)) {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = false
    if (project == null || !isCourseCreator(project)) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    presentation.isEnabledAndVisible = true

    if (course.isMarketplaceRemote) {
      presentation.setText { updateTitle }
    }
    else {
      presentation.setText { uploadTitle }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null || !isCourseCreator(project)) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    val connector = MarketplaceConnector.getInstance()

    if (!checkIfAuthorized(project, e.presentation.text,
                           MarketplaceSettings.INSTANCE.account != null) { connector.doAuthorize() }) {
      return
    }

    val tempFile = FileUtil.createTempFile("marketplace-${course.name}-${course.marketplaceCourseVersion}", ".zip", true)
    val errorMessage = MarketplaceArchiveCreator(project, tempFile.absolutePath).createArchiveWithRemoteCourseVersion()
    if (errorMessage != null) {
      Messages.showErrorDialog(project, errorMessage, message("error.failed.to.create.course.archive"))
      return
    }

    if (!course.isMarketplacePrivate && course.vendor?.name == JB_VENDOR_NAME) {
      val result = showConfirmationDialog(e.presentation.text)
      if (result != Messages.OK) return
    }

    doPush(project, connector, course, tempFile)
  }

  private fun doPush(project: Project, connector: MarketplaceConnector, course: EduCourse, tempFile: File) {
    if (course.isMarketplaceRemote) {
      val courseOnRemote = connector.searchCourse(course.id, course.isMarketplacePrivate)
      // courseOnRemote can be null if it was not validated yet
      if (courseOnRemote == null) {
        CCNotificationUtils.showNotification(project,
                                             uploadNewCourseAction(project, course, connector, tempFile),
                                             message("error.failed.to.update"),
                                             message("marketplace.failed.to.update.no.course"),
                                             NotificationType.ERROR)
        return
      }

      connector.uploadCourseUpdateUnderProgress(project, course, tempFile)
      EduCounterUsageCollector.updateCourse()
    }
    else {
      connector.uploadNewCourseUnderProgress(project, course, tempFile)
      EduCounterUsageCollector.uploadCourse()
    }
  }

  private fun uploadNewCourseAction(project: Project, course: EduCourse, connector: MarketplaceConnector, tempFile: File): AnAction {
    return object : AnAction(message("item.upload.to.0.course.title", MARKETPLACE)) {
      override fun actionPerformed(e: AnActionEvent) {
        connector.uploadNewCourseUnderProgress(project, course, tempFile)
      }
    }
  }

  private fun showConfirmationDialog(actionTitle: String) = Messages.showOkCancelDialog(
    message("marketplace.push.course.confirmation.dialog.message", actionTitle),
    actionTitle,
    actionTitle,
    message("dialog.cancel"),
    null
    )
}