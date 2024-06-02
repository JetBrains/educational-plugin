@file:JvmName("MarketplaceUtils")

package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider.Companion.MARKETPLACE_GROUP_ID
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.marketplace.update.getUpdateInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.update.showUpdateNotification
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.reloadRemoteInfo
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*

private const val DELIMITER = "."

fun getJBAUserInfo(): JBAccountUserInfo? {
  val jbaIdToken = getJBAIdToken() ?: return null

  val parts: List<String> = jbaIdToken.split(DELIMITER)
  if (parts.size < 2) {
    error("JB Account id token data part is malformed")
  }
  val payload = String(Base64.getUrlDecoder().decode(parts[1]))
  return ConnectorUtils.createMapper().readValue(payload, JBAccountUserInfo::class.java)
}

private fun getJBAIdToken(): String? = JBAccountInfoService.getInstance()?.idToken

fun Course.updateCourseItems(project: Project) {
  visitSections { section ->
    section.ensureIdGenerated(project)
  }
  visitLessons { lesson ->
    lesson.ensureIdGenerated(project)
    lesson.visitTasks { task ->
      task.ensureIdGenerated(project)
    }
  }
  YamlFormatSynchronizer.saveRemoteInfo(this)
}

fun StudyItem.ensureIdGenerated(project: Project) {
  reloadRemoteInfo(project)
  generateId()
}

fun Course.setRemoteMarketplaceCourseVersion() {
  val updateInfo = MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(id)
  if (updateInfo != null) {
    incrementMarketplaceCourseVersion(updateInfo.version)
  }
}

fun EduCourse.generateEduId() = "${name}_${vendor?.name}_$languageId"

fun Course.addVendor(): Boolean {
  val currentUser = MarketplaceSettings.INSTANCE.getMarketplaceAccount() ?: return false
  vendor = Vendor(currentUser.userInfo.getFullName())
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
    project.invokeLater {
      if (remoteCourseVersion > marketplaceCourseVersion) {
        if (!isRemoteUpdateFormatVersionCompatible(project, latestUpdateInfo.compatibility.gte)) return@invokeLater
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