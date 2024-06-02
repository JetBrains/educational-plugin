package com.jetbrains.edu.learning.yaml.errorHandling

import com.intellij.CommonBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.decapitalize
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import javax.swing.event.HyperlinkEvent

fun showInvalidConfigNotification(project: Project, configFile: VirtualFile, cause: String) {
  EduNotificationManager
    .create(ERROR, EduCoreBundle.message("yaml.invalid.config.notification.title"), messageWithEditLink(project, configFile, cause))
    .setListener(GoToFileListener(project, configFile))
    .notify(project)
}

private fun messageWithEditLink(project: Project, configFile: VirtualFile, cause: String): String {
  val courseConfig = if (configFile.name == COURSE_CONFIG) {
    configFile
  } else {
    project.courseDir.findChild(COURSE_CONFIG)
  } ?: error(EduCoreBundle.message("yaml.editor.invalid.format.cannot.find.config"))

  val mode = YamlDeserializer.getCourseMode(courseConfig.document.text)

  val mainErrorMessage = "${
    EduCoreBundle.message("yaml.invalid.config.notification.message", pathToConfig(project, configFile))
  }: ${cause.decapitalize()}"

  val editLink = if (mode == CourseMode.STUDENT) {
    ""
  }
  else {
    """<br>
  <a href="">${
      CommonBundle.message("button.edit")
    }</a>"""
  }
  return mainErrorMessage + editLink
}

private fun pathToConfig(project: Project, configFile: VirtualFile): String =
  FileUtil.getRelativePath(project.courseDir.path, configFile.path, VfsUtil.VFS_SEPARATOR_CHAR) ?: error(
    EduCoreBundle.message("yaml.editor.invalid.format.path.not.found", configFile))

private class GoToFileListener(val project: Project, val file: VirtualFile) : NotificationListener {
  override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
    FileEditorManager.getInstance(project).openFile(file, true)
  }
}

