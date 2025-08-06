package com.jetbrains.edu.learning.marketplace.courseStorage.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showInfoNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLogAction
import com.jetbrains.edu.coursecreator.CCUtils.createAndShowCourseVersionDialog
import com.jetbrains.edu.coursecreator.actions.marketplace.courseStorage.CourseStoragePushCourse
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.flatMap
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAuthConnector
import com.jetbrains.edu.learning.marketplace.courseStorage.COURSE_STORAGE
import com.jetbrains.edu.learning.marketplace.courseStorage.changeHost.CourseStorageServiceHost
import com.jetbrains.edu.learning.marketplace.downloadEduCourseFromLink
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.CourseUpdateInfo
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.network.executeParsingErrors
import com.jetbrains.edu.learning.network.toMultipartBody
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.File

abstract class CourseStorageConnector : MarketplaceAuthConnector(), EduCourseConnector {
  override var account: MarketplaceAccount?
    get() = MarketplaceSettings.INSTANCE.getMarketplaceAccount()
    set(value) {
      MarketplaceSettings.INSTANCE.setAccount(value)
    }

  override val platformName: String = COURSE_STORAGE

  override val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(EduCourse::class.java, CourseStorageEduCourseMixin::class.java)
    objectMapper.registerModule(kotlinModule())
    objectMapper
  }

  open val repositoryUrl: String
    get() = CourseStorageServiceHost.getSelectedUrl()

  override fun getCurrentUserInfo(): JBAccountUserInfo? {
    return account?.userInfo
  }

  private fun getRepositoryEndpoints(hubToken: String? = null): CourseStorageRepositoryEndpoints {
    return getEndpoints(account = null, baseUrl = repositoryUrl, accessToken = hubToken)
  }

  override fun searchCourse(courseId: Int, searchPrivate: Boolean): EduCourse? {
    return getRepositoryEndpoints()
      .getCourseDto(courseId)
      .executeHandlingExceptions()
      ?.body()
      ?.apply {
        id = courseId
      }
  }

  override fun loadCourseStructure(course: EduCourse, downloadContext: DownloadCourseContext) {
    val unpackedCourse = loadCourse(course.id, downloadContext)
    course.items = unpackedCourse.items
    course.additionalFiles = unpackedCourse.additionalFiles
    course.marketplaceCourseVersion = unpackedCourse.marketplaceCourseVersion
  }

  override fun loadCourse(
    courseId: Int,
    downloadContext: DownloadCourseContext // TODO(add download context on server side)
  ): EduCourse {
    val updateInfo = getLatestCourseUpdateInfo(courseId) ?: error("Update info for course $courseId is null")
    val link = getCourseDownloadLink(courseId, updateInfo.courseVersion)
    val filePrefix = FileUtil.sanitizeFileName("${COURSE_STORAGE}-${courseId}")
    return downloadEduCourseFromLink(link, filePrefix, courseId)
  }

  private fun getCourseDownloadLink(courseId: Int, updateVersion: Int): String {
    return "$repositoryUrl/api/courses/download?courseId=$courseId&updateVersion=$updateVersion"
  }

  override fun getLatestCourseUpdateInfo(courseId: Int): CourseUpdateInfo? {
    val courseDto = searchCourse(courseId)
    if (courseDto == null) {
      LOG.warn("Course $courseId not found on course storage")
      return null
    }
    return CourseUpdateInfo(courseDto.marketplaceCourseVersion, courseDto.formatVersion)
  }

  override fun getCourseIdFromLink(link: String): Int {
    return link.toIntOrNull() ?: -1
  }

  override fun getCourseInfoByLink(link: String): EduCourse? {
    val courseId = getCourseIdFromLink(link)
    if (courseId == -1) return null
    return searchCourse(courseId)
  }

  fun uploadNewCourse(project: Project, course: EduCourse, file: File) {
    runWithModalProgressBlocking(project, message("action.push.course")) {
      val remoteCourse = uploadCourse(file).onError {
        showFailedToLoadNotification(project, course, it)
        return@runWithModalProgressBlocking
      }

      updateCourseData(course, remoteCourse)

      showInfoNotification(
        project,
        message("marketplace.push.course.successfully.uploaded"),
        message("course.storage.course.successfully.uploaded.message", course.name, course.id),
        NotificationAction.createSimpleExpiring(message("course.storage.course.browse.text")) {
          //TODO(change it to courses pages url after they will be ready)
          EduBrowser.getInstance().browse(repositoryUrl)
        }
      )
      LOG.info("Course ${course.name} has been uploaded to course storage with id = ${course.id}")
    }
  }

  fun uploadCourseUpdate(project: Project, course: EduCourse, file: File) {
    runWithModalProgressBlocking(project, message("push.course.updating.progress.title")) {
      val remoteCourseInfo = getLatestCourseUpdateInfo(course.id)

      if (remoteCourseInfo == null) {
        val convertToLocalAction = NotificationAction.createSimpleExpiring(message("notification.course.creator.access.denied.action")) {
          course.convertToLocal()
          uploadNewCourse(project, course, file)
        }
        showErrorNotification(project, message("notification.course.creator.failed.to.update.course.title"), message("course.storage.failed.to.update.no.course"), convertToLocalAction)
        return@runWithModalProgressBlocking
      }

      if (remoteCourseInfo.courseVersion >= course.marketplaceCourseVersion) {
        val insertedCourseVersion = createAndShowCourseVersionDialog(project, course, message("action.Educational.Educator.CourseStoragePushCourse.text")) ?: return@runWithModalProgressBlocking
        course.marketplaceCourseVersion = insertedCourseVersion
        YamlFormatSynchronizer.saveRemoteInfo(course)
        val pushAction = ActionManager.getInstance().getAction(CourseStoragePushCourse.ACTION_ID)
        showInfoNotification(project, message("marketplace.inserted.course.version.notification", insertedCourseVersion), action = pushAction)
        return@runWithModalProgressBlocking
      }

      val remoteCourse = uploadCourse(file).onError {
        showFailedToLoadNotification(project, course, it)
        return@runWithModalProgressBlocking
      }

      updateCourseData(course, remoteCourse)

      showInfoNotification(
        project,
        message("marketplace.push.course.successfully.updated.title"),
        message("course.storage.course.successfully.updated.message", course.name, course.id, course.marketplaceCourseVersion),
        NotificationAction.createSimpleExpiring(message("course.storage.course.browse.text")) {
          //TODO(change it to courses pages url after they will be ready)
          EduBrowser.getInstance().browse(repositoryUrl)
        }
      )
      LOG.info("Course ${course.name} (id ${course.id}) has been successfully uploaded to course storage with version ${course.marketplaceCourseVersion}")
    }
  }

  private fun showFailedToLoadNotification(project: Project, course: EduCourse, message: String) {
    LOG.warn("Failed to upload course ${course.name}: $message")
    showErrorNotification(project, message("notification.course.creator.failed.to.update.course.title"), action = showLogAction)
  }

  private suspend fun updateCourseData(course: EduCourse, remoteCourse: EduCourse) {
    writeAction {
      course.apply {
        id = remoteCourse.id
        marketplaceCourseVersion = remoteCourse.marketplaceCourseVersion
      }
    }
    YamlFormatSynchronizer.saveRemoteInfo(course)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun uploadCourse(file: File): Result<EduCourse, String> {
    LOG.info("Uploading course from ${file.absolutePath}")
    return getRepositoryEndpoints()
      .uploadCourse(file.toMultipartBody("courseArchive"))
      .executeParsingErrors()
      .flatMap { 
        val course = it.body() ?: return@flatMap Err("Course not found")
        Ok(course)
      }
  }

  companion object {
    private val LOG = logger<CourseStorageConnector>()

    fun getInstance(): CourseStorageConnector = service()
  }
}
