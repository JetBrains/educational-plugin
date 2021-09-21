package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider.Companion.MARKETPLACE_GROUP_ID
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.marketplace.update.getUpdateVersion
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

private const val DATA_DELIMITER = ";"
private const val DELIMITER = "."

fun decodeHubToken(token: String): String? {
  val parts = token.split(DATA_DELIMITER)
  if (parts.size != 2) {
    error("Hub oauth token data part is malformed")
  }
  val userData = parts[0].split(DELIMITER)
  if (userData.size != 4) {
    error("Hub oauth token data part is malformed")
  }
  return if (userData[2].isEmpty()) null else userData[2]
}

fun Course.updateCourseItems() {
  visitSections { section -> section.generateId() }
  visitLessons { lesson ->
    lesson.visitTasks { task ->
      task.generateId()
      task.feedbackLink = null
    }
    lesson.generateId()
  }
  YamlFormatSynchronizer.saveRemoteInfo(this)
}

fun Course.setRemoteMarketplaceCourseVersion() {
  val updateInfo = MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(id)
  if (updateInfo != null) {
    incrementMarketplaceCourseVersion(updateInfo.version)
  }
}

fun addVendor(course: Course): Boolean {
  val currentUser = MarketplaceSettings.INSTANCE.account ?: return false
  course.vendor = Vendor(currentUser.userInfo.name)
  return true
}

fun Course.loadMarketplaceCourseStructure() {
  if (this is EduCourse && isMarketplace && items.isEmpty()) {
    computeUnderProgress(title = EduCoreBundle.message("progress.loading.course")) {
      MarketplaceConnector.getInstance().loadCourseStructure(this)
    }
  }
}

fun EduCourse.checkForUpdates(project: Project, updateForced: Boolean, onFinish: () -> Unit) {
  fun doUpdateInBackground(remoteCourseVersion: Int) {
    runInBackground(title = EduCoreBundle.message("progress.loading.course")) {
      MarketplaceCourseUpdater(project, this, remoteCourseVersion).updateCourse()
    }
  }

  ApplicationManager.getApplication().executeOnPooledThread {
    val remoteCourseVersion = getUpdateVersion()
    runInEdt {
      if (project.isDisposed) return@runInEdt
      if (remoteCourseVersion != null) {
        isUpToDate = false
        if (updateForced) {
          doUpdateInBackground(remoteCourseVersion)
        }
        else {
          showUpdateAvailableNotification(project) {
            doUpdateInBackground(remoteCourseVersion)
          }
          EditorNotifications.getInstance(project).updateAllNotifications()
        }
      }
      onFinish()
    }
  }
}

fun EduCourse.updateFeaturedStatus() {
  if (course.id in MarketplaceListedCoursesIdsLoader.featuredCoursesIds) {
    course.visibility = CourseVisibility.FeaturedVisibility(MARKETPLACE_GROUP_ID)
  }
}
