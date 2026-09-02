package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ImageLoader
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.marketplace.LEARNING_CENTER_CERTIFICATE_PATH
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.changeHost.LearningCenterServiceHost
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.border.Border

private const val CERTIFICATE_IMAGE_PATH = "/certificates/congratulations.png"
private const val CONTENT_WIDTH = 600

/**
 * Notifies the user that a certificate for the course is issued and offers to open it on the Learning Center site.
 */
class CourseCertificateDialog(
  private val project: Project,
  courseName: String,
  private val certificateId: String? = null,
) : CourseCertificateDialogUI, DialogWrapper(project) {

  private val panel = CourseCertificateDialogPanel(courseName)

  init {
    title = EduCoreBundle.message("marketplace.certificate.dialog.title")
    isResizable = false
    setOKButtonText(EduCoreBundle.message("marketplace.certificate.dialog.get.certificate.button.text"))
    init()
    getButton(okAction)?.withExternalLinkArrow(AllIcons.Ide.ExternalLinkArrowWhite)
  }

  override fun createCenterPanel(): JComponent = panel

  override fun createContentPaneBorder(): Border = JBUI.Borders.empty(8, 20)

  override fun doOKAction() {
    if (certificateId == null) { // user is not logged in
      super.doCancelAction()
      MarketplaceConnector.getInstance().doAuthorize(
        {
          with(CourseCertificateManager.getInstance(project)) {
            scope.launch {
              val state = updateCertificateState()
              if (state is CourseCertificationState.Issued) {
                EduBrowser.getInstance().browse(certificateUrl(state.certificateId))
              }
            }
          }
        }
      )
      return
    }
    EduBrowser.getInstance().browse(certificateUrl(certificateId))
    super.doOKAction()
  }

  override fun doCancelAction() {
    // user may close the dialog without logging in
    if (certificateId != null) {
      CourseCertificateBannerManager.getInstance(project).show(certificateId)
    }
    super.doCancelAction()
  }

  override fun dispose() {
    CourseCertificateDialogUIFactory.getInstance(project).dismiss()
    super.dispose()
  }
}

fun certificateUrl(certificateId: String): String =
  "${LearningCenterServiceHost.selectedHost.url}$LEARNING_CENTER_CERTIFICATE_PATH?certificateId=$certificateId"

fun <T : AbstractButton> T.withExternalLinkArrow(icon: Icon): T = apply {
  this.icon = icon
  iconTextGap = 0
  horizontalTextPosition = SwingConstants.LEFT
}

private class CourseCertificateDialogPanel(courseName: String) : JPanel(VerticalFlowLayout(0, 0)) {

  init {
    border = JBUI.Borders.emptyBottom(9)
    add(createHeaderComponent())
    add(Box.createVerticalStrut(JBUI.scale(8)))
    add(createTextComponent(courseName))

    val imageComponent = createImageComponent()
    if (imageComponent != null) {
      add(Box.createVerticalStrut(JBUI.scale(24)))
      add(imageComponent)
    }
  }

  private fun createHeaderComponent(): JComponent = JBLabel(EduCoreBundle.message("marketplace.certificate.dialog.header")).apply {
    font = font.deriveFont(Font.BOLD, JBUIScale.scale(20f))
  }

  private fun createTextComponent(courseName: String): JComponent = JBTextArea().apply {
    text = EduCoreBundle.message("marketplace.certificate.dialog.ready.message", courseName)
    // A text area uses a monospaced font by default, while the dialog text is supposed to use the regular UI font
    font = JBFont.label().deriveFont(JBUIScale.scale(13f))
    border = JBUI.Borders.empty()
    lineWrap = true
    wrapStyleWord = true
    isEditable = false
    isOpaque = false

    preferredSize = Dimension(JBUI.scale(CONTENT_WIDTH), preferredSize.height)
  }

  private fun createImageComponent(): JComponent? {
    val image = ImageLoader.loadFromResource(CERTIFICATE_IMAGE_PATH, CourseCertificateDialog::class.java)
    if (image == null) {
      thisLogger().warn("Failed to load `$CERTIFICATE_IMAGE_PATH`")
      return null
    }
    return JBLabel(JBImageIcon(ImageUtil.scaleImage(image, JBUI.scale(CONTENT_WIDTH), JBUI.scale(338))))
  }
}
