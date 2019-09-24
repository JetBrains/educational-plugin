package com.jetbrains.edu.learning.yaml.errorHandling

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import javax.swing.event.HyperlinkEvent


private const val NOTIFICATION_ID = "Education: invalid config file"

private const val NOTIFICATION_TITLE = "Invalid yaml"

class InvalidConfigNotification(project: Project, configFile: VirtualFile, cause: String) :
  Notification(NOTIFICATION_ID,
               NOTIFICATION_TITLE,
               messageWithEditLink(project, configFile, cause),
               NotificationType.ERROR,
               GoToFileListener(project, configFile))

private fun messageWithEditLink(project: Project, configFile: VirtualFile, cause: String) =
  "File '${pathToConfig(project, configFile)}':<br /> ${cause.decapitalize()} <br /><a href=\"\">Edit</a>"

private fun pathToConfig(project: Project, configFile: VirtualFile) =
  FileUtil.getRelativePath(project.courseDir.path, configFile.path, VfsUtil.VFS_SEPARATOR_CHAR)

private class GoToFileListener(val project: Project, val file: VirtualFile) : NotificationListener {
  override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
    FileEditorManager.getInstance(project).openFile(file, true)
  }
}

