package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.ui.YamlHelpTab
import com.jetbrains.edu.learning.messages.EduCoreBundle

class YamlConfigNotificationProvider(project: Project) : EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware {
  private var cause: String? = null

  init {
    project.messageBus.connect().subscribe(YamlDeserializer.YAML_LOAD_TOPIC,
                                           object : YamlListener {
                                             override fun beforeYamlLoad(configFile: VirtualFile) {
                                               cause = null
                                               EditorNotifications.getInstance(project).updateNotifications(configFile)
                                             }

                                             override fun yamlFailedToLoad(configFile: VirtualFile, exception: String) {
                                               cause = exception
                                               EditorNotifications.getInstance(project).updateNotifications(configFile)
                                             }
                                           })

  }

  override fun getKey(): Key<EditorNotificationPanel> = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    val exception = cause
    if (exception == null) return null
    val panel = EditorNotificationPanel().text(EduCoreBundle.message("notification.yaml.config", exception.decapitalize()))
    panel.createActionLabel(EduCoreBundle.message("notification.yaml.config.help")) { YamlHelpTab.show(project) }
    return panel
  }


  companion object {
    val KEY: Key<EditorNotificationPanel> = Key.create("Failed to apply configuration")
  }
}
