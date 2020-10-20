package com.jetbrains.edu.learning.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle

class EduTaskFileNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {

  override fun getKey(): Key<EditorNotificationPanel> = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    val taskFile = file.getTaskFile(project) ?: return null
    val text = (fileEditor as? TextEditor)?.editor?.document?.text ?: return null
    return if (!taskFile.isValid(text)) {
      val panel = EditorNotificationPanel().text(EduCoreBundle.message("error.solution.cannot.be.loaded"))
      panel.createActionLabel(EduCoreBundle.message("action.reset.request"), "Educational.RefreshTask")
      panel
    }
    else {
      null
    }
  }

  companion object {
    val KEY: Key<EditorNotificationPanel> = Key.create("Solution can't be loaded")
  }
}
