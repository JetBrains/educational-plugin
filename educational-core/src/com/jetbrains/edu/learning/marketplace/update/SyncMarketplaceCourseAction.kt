package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.SyncCourseAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.marketplace.isRemoteUpdateFormatVersionCompatible
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class SyncMarketplaceCourseAction : SyncCourseAction(EduCoreBundle.lazyMessage("action.synchronize.course.text"),
                                                     EduCoreBundle.lazyMessage("action.synchronize.course.description"), null) {

  override fun synchronizeCourse(project: Project) {
    val course = project.course as EduCourse
    runInBackground(title = message("progress.loading.course")) {
      val updateInfo = course.getUpdateInfo() ?: return@runInBackground
      val remoteCourseVersion = updateInfo.version
      if (remoteCourseVersion > course.marketplaceCourseVersion) {
        if (!isRemoteUpdateFormatVersionCompatible(project, updateInfo.compatibility.gte)) return@runInBackground
        MarketplaceCourseUpdater(project, course, remoteCourseVersion).updateCourse()
      }
      else {
        showNotification(project, message("notification.course.up.to.date"), null)
      }
    }

    MarketplaceSolutionLoader.getInstance(project).loadSolutionsInForeground()

    EduCounterUsageCollector.synchronizeCourse(course, EduCounterUsageCollector.SynchronizeCoursePlace.WIDGET)
  }

  override fun isAvailable(project: Project): Boolean {
    if (!EduUtils.isEduProject(project)) {
      return false
    }
    val course = StudyTaskManager.getInstance(project).course
    return course is EduCourse && course.isStudy && course.isMarketplaceRemote
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Marketplace.UpdateCourse"
  }
}