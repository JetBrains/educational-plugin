@file:JvmName("MarketplaceUtils")

package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider.Companion.MARKETPLACE_GROUP_ID
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.marketplace.update.getUpdateInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import com.jetbrains.edu.learning.statistics.DownloadCourseContext.IDE_UI
import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.update.showUpdateNotification

fun EduCourse.setRemoteMarketplaceCourseVersion() {
  val updateInfo = courseConnector.getLatestCourseUpdateInfo(id)
  if (updateInfo != null) {
    incrementMarketplaceCourseVersion(updateInfo.courseVersion)
  }
  else {
    incrementMarketplaceCourseVersion(marketplaceCourseVersion)
  }
}

fun EduCourse.generateEduId() = "${name}_${vendor?.name}_$languageId"

fun Course.addVendor(): Boolean {
  val currentUser = MarketplaceSettings.INSTANCE.getMarketplaceAccount() ?: return false
  vendor = Vendor(currentUser.userInfo.getFullName())
  return true
}

fun Course.loadMarketplaceCourseStructure(downloadCourseContext: DownloadCourseContext = IDE_UI) {
  if (this is EduCourse && isMarketplace && items.isEmpty()) {
    computeUnderProgress(title = EduCoreBundle.message("progress.loading.course")) {
      courseConnector.loadCourseStructure(this, downloadCourseContext)
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
    val latestUpdateInfo = getUpdateInfo() ?: return@executeOnPooledThread
    val remoteCourseVersion = latestUpdateInfo.courseVersion
    project.invokeLater {
      if (remoteCourseVersion > marketplaceCourseVersion) {
        if (!isRemoteUpdateFormatVersionCompatible(project, latestUpdateInfo.formatVersion)) return@invokeLater
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

fun isRemoteUpdateFormatVersionCompatible(project: Project, remoteCourseFormatVersion: Int): Boolean {
  if (remoteCourseFormatVersion > JSON_FORMAT_VERSION) {
    runInEdt {
      if (project.isDisposed) return@runInEdt
      // Suppression needed here because DialogTitleCapitalization is demanded by the superclass constructor, but the plugin naming with
      // the capital letters used in the notification title
      showUpdateNotification(
        project,
        @Suppress("DialogTitleCapitalization") EduCoreBundle.message("notification.update.plugin.title"),
        EduCoreBundle.message("notification.update.plugin.update.course.content")
      )
    }
    return false
  }
  return true
}

fun EduCourse.updateFeaturedStatus() {
  if (course.id in MarketplaceListedCoursesIdsLoader.featuredCoursesIds) {
    course.visibility = CourseVisibility.FeaturedVisibility(MARKETPLACE_GROUP_ID)
  }
}

fun markMarketplaceTheoryTaskAsCompleted(project: Project, task: TheoryTask) {
  runInBackground(project, EduCoreBundle.message("marketplace.posting.theory"), false) {
    MarketplaceSubmissionsConnector.getInstance().markTheoryTaskAsCompleted(task)
  }
}

fun Project.isMarketplaceCourse(): Boolean = course?.isMarketplace == true

fun Project.isMarketplaceStudentCourse(): Boolean = isMarketplaceCourse() && isStudentProject()

fun SolutionSharingPreference?.toBoolean(): Boolean = this == SolutionSharingPreference.ALWAYS

private const val COURSE_STORAGE_ID_LOWER_BOUND = 200_000

// TODO(use {PREFIX-id} ID as id for course storage courses)
fun EduCourse.isFromCourseStorage(): Boolean = id >= COURSE_STORAGE_ID_LOWER_BOUND

val EduCourse.courseConnector: EduCourseConnector
  get() = when {
    isFromCourseStorage() -> CourseStorageConnector.getInstance()
    else -> MarketplaceConnector.getInstance()
  }

fun downloadEduCourseFromLink(link: String, filePrefix: String, courseId: Int): EduCourse {
  val tempFile = FileUtil.createTempFile(filePrefix, ".zip", true)
  logger<EduCourseConnector>().debug("Downloading $courseId course via $link")
  DownloadUtil.downloadAtomically(null, link, tempFile)
  return EduUtilsKt.getLocalCourse(tempFile.path) as? EduCourse ?: error(EduCoreBundle.message("dialog.title.failed.to.unpack.course"))
}