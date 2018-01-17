package com.jetbrains.edu.coursecreator.configuration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.event.HyperlinkEvent


class InvalidConfigNotification(project: Project, configFile: VirtualFile, cause: String) :
  Notification("Edu.InvalidConfig",
               "Invalid configuration",
               "File '${FileUtil.getRelativePath(project.baseDir.path, configFile.path, VfsUtil.VFS_SEPARATOR_CHAR)}': $cause" +
               "<br><a href=\"\">Edit</a>",
               NotificationType.ERROR, GoToFileListener(project, configFile))

private class GoToFileListener(val project: Project, val file: VirtualFile) : NotificationListener {
  override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
    FileEditorManager.getInstance(project).openFile(file, true)
  }
}