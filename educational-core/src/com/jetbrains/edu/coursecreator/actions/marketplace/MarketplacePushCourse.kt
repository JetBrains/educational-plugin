package com.jetbrains.edu.coursecreator.actions.marketplace

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.coursecreator.CCUtils.checkIfAuthorized
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.*
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.io.File

@Suppress("ComponentNotRegistered") // Marketplace.xml
class MarketplacePushCourse(
  private val updateTitle: @NlsActions.ActionText String = message("item.update.on.0.course.title", MARKETPLACE),
  private val uploadTitle: @NlsActions.ActionText String = message("item.upload.to.0.course.title", MARKETPLACE)
) : DumbAwareAction(EduCoreBundle.lazyMessage("gluing.slash", updateTitle, uploadTitle)) {

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

    course.prepareForUpload(project)

    val tempFile = FileUtil.createTempFile("marketplace-${course.name}-${course.marketplaceCourseVersion}", ".zip", true)
    val errorMessage = CourseArchiveCreator(project, tempFile.absolutePath).createArchive()
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
      connector.uploadCourseUpdateUnderProgress(project, course, tempFile)
      EduCounterUsageCollector.updateCourse()
    }
    else {
      connector.uploadNewCourseUnderProgress(project, course, tempFile)
      EduCounterUsageCollector.uploadCourse()
    }
  }

  private fun showConfirmationDialog(actionTitle: @Nls(capitalization = Nls.Capitalization.Title) String) = Messages.showOkCancelDialog(
    message("marketplace.push.course.confirmation.dialog.message", actionTitle),
    actionTitle,
    actionTitle,
    CommonBundle.getCancelButtonText(),
    null
    )

  private fun EduCourse.prepareForUpload(project: Project) {
    if (isMarketplaceRemote) {
      course.setRemoteMarketplaceCourseVersion()
    }

    if (isStepikRemote) {
      // if the course is Stepik remote, that means that the course was opened
      // from Stepik in CC mode with "Edit", and we need to set it's id to 0 before pushing course to marketplace
      course.id = 0
      CCNotificationUtils.showNotification(project, null, message("marketplace.course.converted"),
                                           message("marketplace.not.possible.to.post.updates.to.stepik"))
    }

    if (course.marketplaceCourseVersion == 0) course.marketplaceCourseVersion = 1

    if (!isUnitTestMode) {
      course.updateCourseItems()
    }
    if (course.vendor == null) {
      if (!addVendor(course)) {
        Messages.showErrorDialog(project, message("marketplace.vendor.empty"), message("error.failed.to.create.course.archive"))
        return
      }
    }

    YamlFormatSynchronizer.saveItem(course)
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Educator.MarketplacePushCourse"
  }
}
