package com.jetbrains.edu.coursecreator.archive

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigateToEduFileExtension
import com.jetbrains.edu.learning.navigation.ParsedInCourseLink
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import javax.swing.event.HyperlinkEvent

class FailedToProcessEduFileAsTextualException private constructor(
  val pathInCourse: String,
  @NotificationContent message: String
) : Exception(message) {

  companion object {
    fun create(file: EduFile): FailedToProcessEduFileAsTextualException {
      val pathInCourse = file.pathInCourse

      val message = if (file is TaskFile) {
        EduCoreBundle.message("error.failed.to.process.task.file.as.textual", pathInCourse, file.name)
      }
      else {
        EduCoreBundle.message("error.failed.to.process.additional.file.as.textual", pathInCourse, file.name)
      }

      return FailedToProcessEduFileAsTextualException(pathInCourse, message)
    }
  }
}

class FailedToProcessEduFileAsTextualError(
  exception: FailedToProcessEduFileAsTextualException
) : ExceptionCourseArchiveError<FailedToProcessEduFileAsTextualException>(exception) {

  override fun notification(project: Project, title: String): Notification {

    val makeBinaryAction = NotificationAction.create(
      EduCoreBundle.message("action.make.file.binary")
    ) { e: AnActionEvent, notification: Notification ->
      val project = e.project ?: return@create
      makeBinaryActionPerformed(project, exception.pathInCourse)
      notification.expire()
    }

    @Suppress("DEPRECATION")
    return super.notification(project, title)
      .addAction(makeBinaryAction)
      .setListener(NavigateToEduFileNotificationListener(project))
  }

  private fun makeBinaryActionPerformed(project: Project, pathInCourse: String) {
    val file = pathInCourse.pathInCourseToEduFile(project) ?: return
    file.contents = BinaryContents.EMPTY

    val item = if (file is TaskFile) {
      file.task
    }
    else {
      project.course ?: return
    }

    YamlFormatSynchronizer.saveItem(item)
  }
}

private class NavigateToEduFileNotificationListener(private val project: Project) : NotificationListener.Adapter() {
  override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
    val eduFilePath = e.description ?: return
    val eduFile = eduFilePath.pathInCourseToEduFile(project) ?: return
    val navigationSuccess = NavigateToEduFileExtension.navigateToEduFile(project, eduFile)
    if (!navigationSuccess) {
      notification.expire()
    }
  }
}

private fun String.pathInCourseToEduFile(project: Project): EduFile? {
  val linkToFile = ParsedInCourseLink.parse(project, this) ?: return null
  return when (linkToFile) {
    is ParsedInCourseLink.FileInTask -> linkToFile.taskFile
    is ParsedInCourseLink.CourseAdditionalFile -> linkToFile.additionalFile
    else -> null
  }
}