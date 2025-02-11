package com.jetbrains.edu.learning.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.navigation.ParsedInCourseLink
import com.jetbrains.edu.learning.navigation.ParsedInCourseLink.ItemContainerDirectory
import com.jetbrains.edu.learning.navigation.ParsedInCourseLink.TaskDirectory
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.remoteConfigFileName
import javax.swing.event.HyperlinkEvent

/**
 * Handles links inside notification to study items and navigates to remote yaml config.
 * It's supposed that the link has the format "<a href='[StudyItem.pathInCourse]'>link text</a>"
 *
 * @see [ParsedInCourseLink.parse]
 */
class RemoteConfigNotificationListener(private val project: Project) : NotificationListener.Adapter() {
  override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
    val path = e.description ?: return
    val parsedLink = ParsedInCourseLink.parse(project, path) ?: return
    if (!(parsedLink is ItemContainerDirectory || parsedLink is TaskDirectory)) return

    val configFile = parsedLink.file.findChild(parsedLink.item.remoteConfigFileName) ?: return
    runInEdt {
      FileEditorManager.getInstance(project).openFile(configFile, true)
    }
  }

  companion object {
    fun StudyItem.hyperlinkText(): String = "<a href='${pathInCourse}'>${presentableName}</a>"
  }
}
