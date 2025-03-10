package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.isEditorNotificationIgnored
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.setEditorNotificationIgnored
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import java.util.function.Function
import javax.swing.JComponent

class UserAgreementEditorNotificationsProvider : EditorNotificationProvider, DumbAware {
  /**
   * Show editor notification for Edu project when User Agreement is not accepted
   * And thus, we don't show such a notification in case a regular project is opened (non-Edu)
   */
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isEduYamlProject() || UserAgreementSettings.getInstance().isPluginAllowed || isEditorNotificationIgnored()) return null

    return Function {
      EditorNotificationPanel(EditorNotificationPanel.Status.Warning).apply {
        icon(EducationalCoreIcons.Actions.EduCourse)
        text = EduCoreBundle.message("user.agreement.editor.notification.text")
        createActionLabel(EduCoreBundle.message("user.agreement.editor.notification.action.accept")) {
          UserAgreementManager.getInstance().showUserAgreement(project)
        }
        createActionLabel(EduCoreBundle.message("user.agreement.editor.notification.action.do.not.show.again")) {
          setEditorNotificationIgnored(ignored = true)
          EditorNotifications.getInstance(project).updateNotifications(this@UserAgreementEditorNotificationsProvider)
        }
      }
    }
  }
}