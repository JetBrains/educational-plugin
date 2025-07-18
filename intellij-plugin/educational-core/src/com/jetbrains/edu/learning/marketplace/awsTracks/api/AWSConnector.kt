package com.jetbrains.edu.learning.marketplace.awsTracks.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.HUB_API_PATH
import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAuthConnector
import com.jetbrains.edu.learning.marketplace.awsTracks.changeHost.AWSTracksServiceHost
import com.jetbrains.edu.learning.marketplace.downloadEduCourseFromLink
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.CourseUpdateInfo
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import com.jetbrains.edu.learning.stepik.course.CourseConnector

@Service(Service.Level.APP)
class AWSConnector : MarketplaceAuthConnector(), CourseConnector {
  override var account: MarketplaceAccount?
    get () = MarketplaceSettings.INSTANCE.getMarketplaceAccount()
    set(value) {
      MarketplaceSettings.INSTANCE.setAccount(value)
    }

  override val platformName: String = "AWS"

  override val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(EduCourse::class.java, AWSEduCourseMixin::class.java)
    objectMapper.registerModule(kotlinModule())
    objectMapper
  }

  override val baseUrl: String = "$HUB_AUTH_URL$HUB_API_PATH"

  private val repositoryUrl: String
    get() = AWSTracksServiceHost.getSelectedUrl()

  override fun getCurrentUserInfo(): JBAccountUserInfo? {
    return account?.userInfo
  }

  private fun getRepositoryEndpoints(hubToken: String? = null): AWSRepositoryEndpoints {
    return getEndpoints(baseUrl = repositoryUrl, accessToken = hubToken)
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
    val filePrefix = FileUtil.sanitizeFileName("aws-${courseId}")
    return downloadEduCourseFromLink(link, filePrefix, courseId)
  }

  private fun getCourseDownloadLink(courseId: Int, updateVersion: Int): String {
    return "$repositoryUrl/api/courses/download?courseId=$courseId&updateVersion=$updateVersion"
  }

  override fun getLatestCourseUpdateInfo(courseId: Int): CourseUpdateInfo? {
    val courseDto = searchCourse(courseId)
    if (courseDto == null) {
      error("Course $courseId not found on course storage")
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

  companion object {
    private val LOG = logger<AWSConnector>()

    fun getInstance(): AWSConnector = service()
  }
}
