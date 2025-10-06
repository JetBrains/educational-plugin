package com.jetbrains.edu.coursecreator.archive

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getAdditionalFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigateToConfigEntryForEduFileExtension
import com.jetbrains.edu.learning.navigation.ParsedInCourseLink
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.api.COURSE
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val markAsBinaryAction = NotificationAction.create(
      EduCoreBundle.message("action.mark.file.as.binary")
    ) { e: AnActionEvent, notification: Notification ->
      val project = e.project ?: return@create
      val pathInCourse = exception.pathInCourse

      val eduFile = pathInCourse.pathInCourseToEduFile(project)

      if (eduFile != null) {
        markFileAsBinary(project, eduFile)
        val actionScope = currentThreadCoroutineScope()
        actionScope.launch {
          NavigateToConfigEntryForEduFileExtension.navigateToConfigEntryForEduFile(project, eduFile)
        }
      }

      notification.expire()
    }

    @Suppress("DEPRECATION")
    return super.notification(project, title)
      .addAction(markAsBinaryAction)
      .setListener(NavigateToConfigEntryForEduFileNotificationListener(project))
  }

  private fun markFileAsBinary(project: Project, file: EduFile) {
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

private class NavigateToConfigEntryForEduFileNotificationListener(private val project: Project) : NotificationListener.Adapter() {
  override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
    val eduFilePath = e.description ?: return
    val eduFile = eduFilePath.pathInCourseToEduFile(project) ?: return

    CourseArchiveNotificationService.getInstance(project).navigateToConfigEntryForEduFile(eduFile)
  }
}

private fun String.pathInCourseToEduFile(project: Project): EduFile? {
  val linkToFile = ParsedInCourseLink.parse(project, this)

  if (linkToFile is ParsedInCourseLink.FileInTask) {
    return linkToFile.item.getTaskFile(linkToFile.pathInTask)
  }

  return project.course?.getAdditionalFile(this)
}

@Service(Service.Level.PROJECT)
class CourseArchiveNotificationService(private val project: Project, private val cs: CoroutineScope) {

  fun navigateToConfigEntryForEduFile(eduFile: EduFile) {
    cs.launch {
      withContext(Dispatchers.IO) {
        NavigateToConfigEntryForEduFileExtension.navigateToConfigEntryForEduFile(project, eduFile)
      }
    }
  }

  companion object {
    fun getInstance(project: Project): CourseArchiveNotificationService = project.service()
  }
}