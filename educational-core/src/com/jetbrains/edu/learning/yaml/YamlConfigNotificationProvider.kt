package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotificationProvider.CONST_NULL
import com.jetbrains.edu.learning.decapitalize
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Function
import javax.swing.JComponent

class YamlConfigNotificationProvider : EditorNotificationProvider, DumbAware {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    @Suppress("DEPRECATION") // BACKCOMPAT: 2022.2, change to null
    val exception = YamlLoadingErrorManager.getInstance(project).getLoadingErrorForFile(file) ?: return CONST_NULL
    return Function {
      EditorNotificationPanel().text(EduCoreBundle.message("notification.yaml.config", exception.decapitalize()))
    }
  }
}
