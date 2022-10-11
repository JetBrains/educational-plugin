package com.jetbrains.edu.learning.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotificationProvider.CONST_NULL
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Function
import javax.swing.JComponent

class EduTaskFileNotificationProvider : EditorNotificationProvider {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    val taskFile = file.getTaskFile(project) ?: return CONST_NULL
    return Function { fileEditor ->
      val text = (fileEditor as? TextEditor)?.editor?.document?.text ?: return@Function null
      if (!taskFile.isValid(text)) {
        val panel = EditorNotificationPanel().text(EduCoreBundle.message("error.solution.cannot.be.loaded"))
        panel.createActionLabel(EduCoreBundle.message("action.Educational.RefreshTask.text"), "Educational.RefreshTask")
        panel
      }
      else {
        null
      }
    }
  }
}
