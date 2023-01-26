@file:JvmName("MarketplaceUtils")

package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider.Companion.MARKETPLACE_GROUP_ID
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.marketplace.update.getUpdateInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.update.UpdateNotification
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
  return userData[2].ifEmpty { null }
}

fun Course.updateCourseItems() {
  visitSections { section -> section.generateId() }
  visitLessons { lesson ->
    lesson.visitTasks { task ->
      task.generateId()
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

fun Course.generateEduId() {
  if (generatedEduId != null) return

  generatedEduId = "${name}_${vendor?.name}_$programmingLanguage"
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
    val latestUpdateInfo = getUpdateInfo() ?: return@executeOnPooledThread
    val remoteCourseVersion = latestUpdateInfo.version
    runInEdt {
      if (project.isDisposed) return@runInEdt
      if (remoteCourseVersion > marketplaceCourseVersion) {
        if (!isRemoteUpdateFormatVersionCompatible(project, latestUpdateInfo.compatibility.gte)) return@runInEdt
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
      @Suppress("DialogTitleCapitalization")
      UpdateNotification(EduCoreBundle.message("notification.update.plugin.title"),
                         EduCoreBundle.message("notification.update.plugin.update.course.content")).notify(project)
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

fun markTheoryTaskAsCompleted(project: Project, task: TheoryTask) {
  if (!isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS)) return
  runInBackground(project, EduCoreBundle.message("marketplace.posting.theory"), false) {
    MarketplaceSubmissionsConnector.getInstance().markTheoryTaskAsCompleted(project, task)
  }
}
