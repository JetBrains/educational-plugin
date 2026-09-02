package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeRootPaneNorthExtension
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.components.ActionLink
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.TestOnly
import javax.swing.JComponent

/**
 * Keeps track of whether the certificate banner should be shown in the current project.
 *
 * The banner is shown when the user closes [CourseCertificateDialog] without opening the certificate,
 * so they still have a way to get it. The dialog is shown only once per certificate,
 * so the banner survives project reopening and is hidden only when the user dismisses it explicitly.
 */
@Service(Service.Level.PROJECT)
@State(name = "CourseCertificateBanner", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CourseCertificateBannerManager :
  SerializablePersistentStateComponent<CourseCertificateBannerManager.BannerState>(BannerState()),
  EduTestAware {

  private val _certificateId: MutableStateFlow<String?> = MutableStateFlow(null)

  val certificateId: StateFlow<String?> = _certificateId.asStateFlow()

  fun show(certificateId: String) {
    updateState { it.copy(certificateId = certificateId) }
    _certificateId.value = certificateId
  }

  fun dismiss() {
    updateState { BannerState(certificateId = null) }
    _certificateId.value = null
  }

  override fun loadState(state: BannerState) {
    super.loadState(state)
    _certificateId.value = state.certificateId
  }

  @TestOnly
  override fun cleanUpState() {
    updateState { BannerState() }
    _certificateId.value = null
  }

  @Serializable
  data class BannerState(
    val certificateId: String? = null,
  )

  companion object {
    fun getInstance(project: Project): CourseCertificateBannerManager = project.service()
  }
}

class CourseCertificateBannerExtension : IdeRootPaneNorthExtension {

  override val key: String
    get() = KEY

  override fun component(project: Project, isDocked: Boolean, statusBar: StatusBar): Flow<JComponent?> =
    CourseCertificateBannerManager.getInstance(project).certificateId.map { certificateId ->
      certificateId?.let { createBanner(project, it) }
    }

  private fun createBanner(project: Project, certificateId: String): JComponent =
    object : EditorNotificationPanel(Status.Success) {
      init {
        text = EduCoreBundle.message("marketplace.certificate.banner.message")
        myLinksPanel.add(createCertificateLink(certificateId, project))
        setCloseAction { CourseCertificateBannerManager.getInstance(project).dismiss() }
      }
    }

  private fun createCertificateLink(certificateId: String, project: Project): ActionLink =
    ActionLink(EduCoreBundle.message("marketplace.certificate.banner.get.certificate.link.text")) {
      EduBrowser.getInstance().browse(certificateUrl(certificateId))
      CourseCertificateBannerManager.getInstance(project).dismiss()
    }.withExternalLinkArrow(AllIcons.Ide.External_link_arrow)

  companion object {
    private const val KEY: String = "edu.course.certificate.banner"
  }
}
