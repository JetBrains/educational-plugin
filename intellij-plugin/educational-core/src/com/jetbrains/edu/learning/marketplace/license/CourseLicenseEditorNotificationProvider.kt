package com.jetbrains.edu.learning.marketplace.license

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.actions.CheckLicenseAction
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Function
import javax.swing.JComponent

class CourseLicenseEditorNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isEduProject()) return null
    if (!LicenseLinkSettings.isLicenseRequired(project)) return null
    val licenseState = LicenseChecker.getInstance(project).licenseState.value ?: return null

    return Function {
      val notificationText = when (licenseState) {
        LicenseState.VALID -> return@Function null
        LicenseState.INVALID -> EduCoreBundle.message("license.notification.invalid.text")
        LicenseState.ERROR -> EduCoreBundle.message("license.notification.error.text")
      }
      EditorNotificationPanel(EditorNotificationPanel.Status.Error).apply {
        text = notificationText
        when (licenseState) {
          LicenseState.INVALID -> {
            val link = OpenOnSiteLinkSettings.getInstance(project).link ?: return@apply
            createActionLabel(EduCoreBundle.message("course.dialog.learn.more")) {
              EduBrowser.getInstance().browse(link)
            }
          }
          LicenseState.ERROR -> {
            createActionLabel(EduCoreBundle.message("retry"), CheckLicenseAction.ACTION_ID)
          }
          else -> {}
        }
      }
    }
  }
}