package com.jetbrains.edu.learning.marketplace.license

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.actions.CheckLicenseAction
import com.jetbrains.edu.learning.marketplace.license.CourseLicenseEditorNotificationProvider.LicenseNotificationTexts.ERROR
import com.jetbrains.edu.learning.marketplace.license.CourseLicenseEditorNotificationProvider.LicenseNotificationTexts.EXPIRED
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Function
import java.util.function.Supplier
import javax.swing.JComponent

class CourseLicenseEditorNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isEduProject()) return null
    if (!LicenseLinkSettings.isLicenseRequired(project)) return null
    val licenseState = LicenseChecker.getInstance(project).licenseState.value ?: return null

    val (notificationText, retryActionText) = licenseState.toNotificationTexts() ?: return null
    return Function {
      EditorNotificationPanel(EditorNotificationPanel.Status.Error).apply {
        text = notificationText.get()
        if (licenseState == LicenseState.EXPIRED) {
          // if the license is expired -> show a link to a learning center
          val link = OpenOnSiteLinkSettings.getInstance(project).link ?: return@apply
          createActionLabel(EduCoreBundle.message("course.dialog.learn.more")) {
            EduBrowser.getInstance().browse(link)
          }
        }
        // if license expired or error happened -> give an option to retry the check
        createActionLabel(retryActionText.get(), CheckLicenseAction.ACTION_ID)
      }
    }
  }

  private enum class LicenseNotificationTexts(
    @NlsContexts.Label val text: Supplier<String>,
    @NlsContexts.LinkLabel val retryActionText: Supplier<String>
  ) {
    ERROR(
      EduCoreBundle.lazyMessage("license.notification.error.text"),
      EduCoreBundle.lazyMessage("retry"),
    ),

    EXPIRED(
      EduCoreBundle.lazyMessage("license.notification.invalid.text"),
      EduCoreBundle.lazyMessage("license.notification.invalid.check.license.text"),
    );

    operator fun component1() = text
    operator fun component2() = retryActionText
  }

  private fun LicenseState.toNotificationTexts(): LicenseNotificationTexts? = when (this) {
    LicenseState.VALID -> null
    LicenseState.ERROR -> ERROR
    LicenseState.EXPIRED -> EXPIRED
  }
}