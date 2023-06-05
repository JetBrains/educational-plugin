package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.update.EduCourseUpdater
import com.jetbrains.edu.learning.update.UpdateNotification

@Suppress("DuplicatedCode")
class MarketplaceCourseUpdaterNew(project: Project, override val course: EduCourse) : EduCourseUpdater(project, course) {
  override fun areUpdatesAvailable(): Boolean {
    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }

    val updateInfo = course.getUpdateInfo() ?: return false
    val remoteCourseVersion = updateInfo.version
    if (!isRemoteUpdateFormatVersionCompatible(updateInfo.compatibility.gte)) return false
    return remoteCourseVersion > course.marketplaceCourseVersion
  }
  override fun getCourseFromServer(): EduCourse? {
    val courseFromServer = MarketplaceConnector.getInstance().searchCourse(course.id, course.isMarketplacePrivate)
    if (courseFromServer != null) {
      MarketplaceConnector.getInstance().loadCourseStructure(courseFromServer)
    }
    return courseFromServer
  }

  override fun doUpdate() {
    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }

    val courseFromServer = getCourseFromServer()
    TODO()
  }

  private fun isRemoteUpdateFormatVersionCompatible(remoteCourseFormatVersion: Int): Boolean {
    if (remoteCourseFormatVersion > JSON_FORMAT_VERSION) {
      runInEdt {
        if (project.isDisposed) return@runInEdt
        // Suppression needed here because DialogTitleCapitalization is demanded by the superclass constructor, but the plugin naming with
        // the capital letters used in the notification title
        @Suppress("DialogTitleCapitalization")
        UpdateNotification(
          EduCoreBundle.message("notification.update.plugin.title"),
          EduCoreBundle.message("notification.update.plugin.update.course.content")
        ).notify(project)
      }
      return false
    }
    return true
  }
}