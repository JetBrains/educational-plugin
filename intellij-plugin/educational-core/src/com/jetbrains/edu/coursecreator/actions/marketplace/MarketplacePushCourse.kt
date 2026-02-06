package com.jetbrains.edu.coursecreator.actions.marketplace

import com.intellij.CommonBundle
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.addGluingSlash
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.CCUtils.prepareForUpload
import com.jetbrains.edu.coursecreator.VendorError
import com.jetbrains.edu.coursecreator.archive.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.archive.showNotification
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.JB_VENDOR_NAME
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showLoginNeededNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showReloginToJBANeededNotification
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector.CourseUploadingData
import com.jetbrains.edu.learning.marketplace.api.UserOrganization
import com.jetbrains.edu.learning.marketplace.defaultVendor
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.notification.EduNotificationManager.openLinkAction
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.util.concurrent.CompletableFuture

@Suppress("ComponentNotRegistered") // Marketplace.xml
class MarketplacePushCourse(
  private val updateTitle: @NlsActions.ActionText String = message("item.update.on.0.course.title", MARKETPLACE),
  private val uploadTitle: @NlsActions.ActionText String = message("item.upload.to.0.course.title", MARKETPLACE)
) : DumbAwareAction(addGluingSlash(updateTitle, uploadTitle)) {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = false
    if (project == null || !isCourseCreator(project)) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (course.isMarketplaceRemote && course.isFromCourseStorage()) return

    presentation.isEnabledAndVisible = true

    if (course.isMarketplaceRemote) {
      presentation.setText { updateTitle }
    }
    else {
      presentation.setText { uploadTitle }
    }
  }

 override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null || !isCourseCreator(project)) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    val connector = MarketplaceConnector.getInstance()

    // We need an account to receive the hub token, and only for this purpose
    val currentAccount = connector.account
    if (currentAccount == null) {
      showLoginNeededNotification(project, e.presentation.text) { connector.doAuthorize() }
      return
    }

    CompletableFuture.supplyAsync({ connector.loadHubToken() }, ProcessIOExecutorService.INSTANCE).handle { hubToken, exception ->
      if (exception != null) {
        LOG.warn("Hub authorization failed: ${exception.message}", exception)
        EduNotificationManager.showErrorNotification(
          project,
          message("marketplace.push.course.failed.title"),
          message("marketplace.push.course.failed.text", course.name)
        )
      }
      /**
       * Workaround: while awaiting https://youtrack.jetbrains.com/issue/HUB-11929,
       * let's always prompt action to re-login whenever `hubToken` is null as it might help
       */
      if (hubToken == null) {
        LOG.error("Login failed: JetBrains account token is null")
        val jbAccountInfoService = JBAccountInfoService.getInstance() ?: return@handle
        return@handle showReloginToJBANeededNotification(connector.invokeJBALoginAction(jbAccountInfoService))
      }
      project.invokeLater {
        prepareAndPush(project, course, connector, e.presentation.text, hubToken)
      }
    }
  }

  private fun prepareAndPush(project: Project, course: EduCourse, connector: MarketplaceConnector, actionName: String, hubToken: String) {
    // A bit hacky way to keep reference to organization and pass it further not to load it again
    // TODO: refactor it
    var organization: UserOrganization? = null
    val preparedSuccessfully = course.prepareForUpload(project) {
      when (val result = processVendor(hubToken)) {
        is Err -> result.error
        is Ok -> {
          organization = result.value
          null
        }
      }
    }
    if (!preparedSuccessfully) return

    val courseArchiveFile = FileUtil.createTempFile("marketplace-${course.name}-${course.marketplaceCourseVersion}", ".zip", true)
    val error = CourseArchiveCreator(project, courseArchiveFile.toPath()).createArchive(course)
    if (error != null) {
      error.showNotification(project, message("error.failed.to.create.course.archive.notification.title"))
      return
    }

    if (!course.isMarketplacePrivate && course.vendor?.name == JB_VENDOR_NAME) {
      val result = showConfirmationDialog(actionName)
      if (result != Messages.OK) return
    }

    // It's impossible to have null here because otherwise we would break the function after the preparation phase
    if (organization == null) error("unreachable")
    doPush(project, connector, hubToken, CourseUploadingData(course, courseArchiveFile, organization))
  }

  private suspend fun EduCourse.processVendor(hubToken: String): Result<UserOrganization, VendorError> {
    val currentVendor = vendor ?: defaultVendor()
    if (currentVendor == null) {
      return Err(CCUtils.emptyVendorError())
    }

    val organizations = MarketplaceConnector.getInstance().loadUserOrganizations(hubToken)

    val userOrganization = when {
      organizations == null -> {
        return Err(VendorError(message("marketplace.push.course.error.vendor.failed.to.fetch")))
      }
      organizations.isEmpty() -> {
        return Err(VendorError(
          message("marketplace.push.course.error.vendor.do.not.exist.on.marketplace"),
          openLinkAction(message("marketplace.push.course.error.notification.action.create.vendor"), "https://plugins.jetbrains.com/vendor/new")
        ))
      }
      else -> {
        // It's important to check both `publicName` and `name` because
        // before we used `publicName` as a vendor name for personal vendors
        // and `name` for organizations (i.e. `JetBrains` instead of `JetBrains s.r.o.`)
        val userOrganization = organizations.find { it.publicName == currentVendor.name || it.name == currentVendor.name }
        if (userOrganization == null) {
          return Err(VendorError(
            message("marketplace.push.course.error.vendor.no.matching"),
            openLinkAction(message("marketplace.push.course.error.notification.action.open.vendor.list"), "https://plugins.jetbrains.com/author/me/organizations")
          ))
        }
        userOrganization
      }
    }

    writeAction {
      course.vendor = userOrganization.toVendor()
    }
    return Ok(userOrganization)
  }

  private fun UserOrganization.toVendor(): Vendor {
    return Vendor(publicName, email.takeIf { showEmail }, url)
  }

  private fun doPush(project: Project, connector: MarketplaceConnector, hubToken: String, courseUploadingData: CourseUploadingData) {
    if (courseUploadingData.course.isMarketplaceRemote) {
      connector.uploadCourseUpdateUnderProgress(project, hubToken, courseUploadingData)
      EduCounterUsageCollector.updateCourse()
    }
    else {
      connector.uploadNewCourseUnderProgress(project, hubToken, courseUploadingData)
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

  companion object {
    private val LOG = logger<MarketplacePushCourse>()

    @NonNls
    const val ACTION_ID: String = "Educational.Educator.MarketplacePushCourse"
  }
}
