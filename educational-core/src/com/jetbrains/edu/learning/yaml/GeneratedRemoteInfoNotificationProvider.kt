package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isRemoteConfigFile
import org.jetbrains.annotations.NonNls

class GeneratedRemoteInfoNotificationProvider(val project: Project) :
  EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware {

  companion object {
    @NonNls
    private const val KEY_NAME = "Edu.generatedRemoteInfo"
    val KEY: Key<EditorNotificationPanel> = Key.create(KEY_NAME)
    private val NOTIFICATION_TEXT: String = EduCoreBundle.message("yaml.remote.config.notification")
  }

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (isRemoteConfigFile(file)) {
      val panel = EditorNotificationPanel()
      panel.text = NOTIFICATION_TEXT
      return panel
    }
    return null
  }
}
