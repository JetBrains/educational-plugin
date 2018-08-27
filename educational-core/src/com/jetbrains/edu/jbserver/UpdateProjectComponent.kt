package com.jetbrains.edu.jbserver

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse


class UpdateProjectComponent(val project: Project): ProjectComponent {

  override fun projectOpened() {

    if (project.isDisposed) return
    if (!EduUtils.isStudyProject(project)) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return

    StartupManager.getInstance(project).runWhenProjectIsInitialized {

      if (!ServerConnector.isCourseUpdated(course)) return@runWhenProjectIsInitialized

      // todo : should we use other ui element here ?

      val confirmUpdate = MessageDialogBuilder.yesNo(
        "Course update available",
        "Update for course `${course.name}` is available. Please update the course, otherwise some functionality may not work."
      ).yesText("Update").noText("Continue without update")

      if (confirmUpdate.show() == Messages.YES) try {
        ServerConnector.getCourseUpdate(course)
        EduUtils.notify("Course update", "Course `${course.name}` updated successfully", NotificationType.INFORMATION)
      } catch (e: Exception) {
        EduUtils.notify("Course update", "Error occured while updating course `${course.name}`", NotificationType.ERROR)
      }

    }

  }

}
