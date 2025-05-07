package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.SyncCourseAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import org.jetbrains.annotations.NonNls

class SyncHyperskillCourseAction : SyncCourseAction(
  EduCoreBundle.lazyMessage("hyperskill.update.project"),
  EduCoreBundle.lazyMessage("hyperskill.update.project"), null
) {

  override val loginWidgetText: String
    get() = EduCoreBundle.message("hyperskill.action.synchronize.project")

  override fun synchronizeCourse(project: Project) {
    val course = project.course as HyperskillCourse
    HyperskillCourseUpdater(project, course).updateCourse { isUpdated ->
      if (!isUpdated) {
        EduNotificationManager.showInfoNotification(
          project,
          EduCoreBundle.message("update.nothing.to.update"),
          EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT),
        )
      }

      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }

    EduCounterUsageCollector.synchronizeCourse(course, EduCounterUsageCollector.SynchronizeCoursePlace.WIDGET)
  }

  override fun isAvailable(project: Project): Boolean {
    if (!project.isStudentProject()) return false
    return project.course is HyperskillCourse
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Hyperskill.UpdateCourse"
  }
}