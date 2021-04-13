package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.update.CourseUpdateChecker

@Service
class MarketplaceUpdateChecker(project: Project) : CourseUpdateChecker(project) {

  override fun courseCanBeUpdated(): Boolean {
    val marketplaceCourse = course as? EduCourse ?: return false
    return marketplaceCourse.isMarketplaceRemote || marketplaceCourse.isStudy && marketplaceCourse.isMarketplace
  }

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val marketplaceCourse = course as? EduCourse ?: return

    ApplicationManager.getApplication().executeOnPooledThread {
      val remoteCourseVersion = marketplaceCourse.getUpdateVersion()
      runInEdt {
        if (project.isDisposed) return@runInEdt
        if (remoteCourseVersion != null) {
          marketplaceCourse.isUpToDate = false
          showUpdateAvailableNotification(project) {
            runInBackground(title = message("progress.loading.course")) {
              MarketplaceCourseUpdater(project, marketplaceCourse, remoteCourseVersion).updateCourse()
            }
          }
          EditorNotifications.getInstance(project).updateAllNotifications()
        }
        onFinish()
      }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): MarketplaceUpdateChecker {
      return project.service()
    }
  }
}
