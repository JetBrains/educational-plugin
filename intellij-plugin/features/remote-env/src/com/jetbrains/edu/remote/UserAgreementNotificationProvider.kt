package com.jetbrains.edu.remote

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.agreement.UserAgreementManager
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.marketplace.isMarketplaceStudentCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Function
import javax.swing.JComponent

class UserAgreementNotificationProvider : EditorNotificationProvider, DumbAware {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!RemoteEnvHelper.isRemoteDevServer() || !project.isMarketplaceStudentCourse()) return null
    val agreementState = UserAgreementSettings.getInstance().pluginAgreement
    if (agreementState) return null

    return Function {
      val panel = EditorNotificationPanel()
      panel.text = EduCoreBundle.message("user.agreement.remote.editor.notification")
      panel.createActionLabel(EduCoreBundle.message("user.agreement.remote.editor.notification.details")) {
        UserAgreementManager.getInstance().showUserAgreement(project)
      }
      panel
    }
  }
}