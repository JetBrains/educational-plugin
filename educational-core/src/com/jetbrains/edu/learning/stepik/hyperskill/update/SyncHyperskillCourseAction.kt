package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.SyncCourseAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillNotificationGroup

@Suppress("ComponentNotRegistered")
class SyncHyperskillCourseAction : SyncCourseAction(EduCoreBundle.lazyMessage("hyperskill.update.project"),
                                                    EduCoreBundle.lazyMessage("hyperskill.update.project"), null) {

  override val loginWidgetText: String
    get() = EduCoreBundle.message("hyperskill.action.synchronize.project")

  override fun synchronizeCourse(project: Project) {
    val course = project.course as HyperskillCourse
    HyperskillCourseUpdater(project, course).updateCourse { isUpdated ->
      if (!isUpdated) {
        showNothingToUpdateNotification(project)
      }
    }

    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()

    EduCounterUsageCollector.synchronizeCourse(course, EduCounterUsageCollector.SynchronizeCoursePlace.WIDGET)
  }

  override fun isAvailable(project: Project): Boolean {
    if (!EduUtils.isStudentProject(project)) return false
    if (project.course !is HyperskillCourse) return false
    return true
  }

  private fun showNothingToUpdateNotification(project: Project) {
    Notification(
      hyperskillNotificationGroup.displayId,
      EduCoreBundle.message("update.nothing.to.update"),
      EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT),
      NotificationType.INFORMATION
    ).notify(project)
  }
}