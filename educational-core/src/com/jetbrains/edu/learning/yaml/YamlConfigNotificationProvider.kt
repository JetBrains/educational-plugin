package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotificationProvider.CONST_NULL
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.ui.YamlHelpTab
import com.jetbrains.edu.learning.decapitalize
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Function
import javax.swing.JComponent

class YamlConfigNotificationProvider(project: Project) : EditorNotificationProvider, DumbAware {
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

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    val exception = cause
    if (exception == null) return CONST_NULL
    return Function {
      val panel = EditorNotificationPanel().text(EduCoreBundle.message("notification.yaml.config", exception.decapitalize()))
      panel.createActionLabel(EduCoreBundle.message("notification.yaml.config.help")) { YamlHelpTab.show(project) }
      panel
    }
  }
}
