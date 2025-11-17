package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.actions.InstallHyperskillPluginAction
import java.util.function.Function
import javax.swing.JComponent

class InstallHyperskillPluginEditorNotificationsProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent>? {
    if (!project.isHyperskillProject) return null
    if (!needInstallHyperskillPlugin()) return null

    return Function {
      EditorNotificationPanel(EditorNotificationPanel.Status.Error).apply {
        text = EduCoreBundle.message("hyperskill.new.plugin.editor.notification.install.text")
        // Action label text should have sentence capitalization, but `Hyperskill Academy` is a name of the plugin
        @Suppress("DialogTitleCapitalization")
        createActionLabel(EduCoreBundle.message("hyperskill.new.plugin.editor.notification.install.action.text"), InstallHyperskillPluginAction.ACTION_ID)
      }
    }
  }
}