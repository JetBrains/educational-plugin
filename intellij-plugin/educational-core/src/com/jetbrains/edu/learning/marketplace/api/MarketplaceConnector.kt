package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.asSafely
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showInfoNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLogAction
import com.jetbrains.edu.coursecreator.CCUtils.createAndShowCourseVersionDialog
import com.jetbrains.edu.coursecreator.actions.marketplace.MarketplacePushCourse
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.LICENSE_URL
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.MARKETPLACE_PLUGIN_URL
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedToFindMarketplaceCourseOnRemoteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showLoginNeededNotification
import com.jetbrains.edu.learning.marketplace.api.GraphqlQuery.LOADING_STEP
import com.jetbrains.edu.learning.marketplace.downloadEduCourseFromLink
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.CourseUpdateInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.network.executeCall
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.network.toMultipartBody
import com.jetbrains.edu.learning.network.toPlainTextRequestBody
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.CompletableFuture

abstract class MarketplaceConnector : MarketplaceAuthConnector(), EduCourseConnector {
  /**
   * It is recommended to use the static methods of the [com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount] class
   * instead of relying on the 'account' property. However, there are still certain things, such as the actual name of the JBA user,
   * that need to be retrieved from this property. Also, this property is used for retrieving Hub token.
   */
  override var account: MarketplaceAccount?
    get () = MarketplaceSettings.INSTANCE.getMarketplaceAccount()
    set(value) {
      MarketplaceSettings.INSTANCE.setAccount(value)
    }

  override val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(EduCourse::class.java, MarketplaceEduCourseMixin::class.java)
    objectMapper.registerModule(kotlinModule())
    objectMapper
  }

  override val platformName: String = MARKETPLACE

  protected abstract val repositoryUrl: String

  private fun getRepositoryEndpoints(hubToken: String? = null): MarketplaceRepositoryEndpoints {
    return getEndpoints(account = null, baseUrl = repositoryUrl, accessToken = hubToken)
  }

  // Get requests:
  override fun getCurrentUserInfo(): JBAccountUserInfo? {
    return account?.userInfo
  }

  fun searchCourses(): List<EduCourse> {
    val allCourses = mutableListOf<EduCourse>()
    allCourses.addAll(searchCourses(false))
    if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_PRIVATE_COURSES)) {
      allCourses.addAll(searchCourses(true))
    }
    return allCourses
  }

  private fun searchCourses(searchPrivate: Boolean): List<EduCourse> {
    var offset = 0
    val courses = mutableListOf<EduCourse>()

    do {
      val (loadedCourses, total) = loadCourses(QueryData(GraphqlQuery.search(offset, searchPrivate))) ?: return courses
      if (loadedCourses.isEmpty()) return courses
      courses.addAll(loadedCourses)
      offset += LOADING_STEP
    }
    while (courses.size < total)

    return courses
  }

  override fun searchCourse(courseId: Int, searchPrivate: Boolean): EduCourse? {
    val course = loadCourses(QueryData(GraphqlQuery.searchById(courseId, searchPrivate)))?.courses?.firstOrNull() ?: return null
    course.id = courseId
    return course
  }

  private fun loadCourses(query: QueryData): LoadedCourses? {
    val response = getRepositoryEndpoints().search(query).executeHandlingExceptions()
    val coursesInfoList = response?.body()?.data?.myCoursesInfoList ?: return null
    val courses = coursesInfoList.courses
      .updateFormatVersions()
    return LoadedCourses(courses, coursesInfoList.total)
  }

  private fun List<EduCourse>.updateFormatVersions(): List<EduCourse> {
    val courseIds = map { it.id }
    val updates = getUpdateInfoList(courseIds) ?: return emptyList()
    val courseIdJsonVersionMap: Map<Int, Int> = updates.associateBy(UpdateInfo::pluginId) { it.compatibility.gte }
    return mapNotNull { course ->
      val courseFormatVersion = courseIdJsonVersionMap[course.id]
      if (courseFormatVersion == null) {
        LOG.error("No UpdateInfo found for course ${course.name}")
        return@mapNotNull null
      }
      course.formatVersion = courseFormatVersion
      course
    }
  }

  override fun getLatestCourseUpdateInfo(courseId: Int): CourseUpdateInfo? {
    val updateInfo = getMarketplaceCourseUpdateInfo(courseId) ?: return null
    return CourseUpdateInfo(updateInfo.version, updateInfo.compatibility.gte)
  }

  @VisibleForTesting
  fun getMarketplaceCourseUpdateInfo(courseId: Int): UpdateInfo? {
    val updateInfoList = getUpdateInfoList(listOf(courseId))
    if (updateInfoList == null) {
      error("Update info list for course $courseId is null")
    }
    else {
      return updateInfoList.firstOrNull()
    }
  }

  private fun getUpdateInfoList(courseIds: List<Int>): List<UpdateInfo>? {
    val response = getRepositoryEndpoints().getUpdateId(QueryData(GraphqlQuery.lastUpdatesList(courseIds))).executeHandlingExceptions()
    return response?.body()?.data?.updates?.updateInfoList
  }

  override fun loadCourseStructure(course: EduCourse, downloadContext: DownloadCourseContext) {
    val unpackedCourse = loadCourse(course.id, downloadContext)
    course.items = unpackedCourse.items
    course.additionalFiles = unpackedCourse.additionalFiles
    course.marketplaceCourseVersion = unpackedCourse.marketplaceCourseVersion
  }

  override fun loadCourse(courseId: Int, downloadContext: DownloadCourseContext): EduCourse {
    val buildNumber = ApplicationInfoImpl.getShadowInstanceImpl().pluginCompatibleBuild
    val uuid = PluginDownloader.getMarketplaceDownloadsUUID()
    val updateInfo = getMarketplaceCourseUpdateInfo(courseId) ?: error("Update info for course $courseId is null")

    val link = "$repositoryUrl/plugin/download?updateId=${updateInfo.updateId}&uuid=$uuid&build=$buildNumber&source=$downloadContext"
    val filePrefix = FileUtil.sanitizeFileName("marketplace-${courseId}")
    return downloadEduCourseFromLink(link, filePrefix, courseId)
  }

  private fun uploadUnderProgress(message: String, uploadAction: () -> Unit) =
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtilsKt.execCancelable {
          uploadAction()
        }
      }, message, true, null)

  fun uploadNewCourseUnderProgress(project: Project, course: EduCourse, file: File, hubToken: String) {
    uploadUnderProgress(message("action.push.course")) {
      uploadNewCourse(project, course, file, hubToken)
    }
  }

  private fun uploadNewCourse(project: Project, course: EduCourse, file: File, hubToken: String) {
    LOG.info("Uploading new course from ${file.absolutePath}")

    val response = getRepositoryEndpoints(hubToken).uploadNewCourse(file.toMultipartBody(), LICENSE_URL.toPlainTextRequestBody())
      .executeUploadParsingErrors(project, message("notification.course.creator.failed.to.upload.course.title"), showLogAction)
      .onError {
        LOG.error("Failed to upload course ${course.name}: $it")
        return
      }

    val uploadCourseResponse = response.body().asSafely<SuccessCourseUploadResponse>()
    if (uploadCourseResponse == null) {
      showErrorNotification(project, message("notification.course.creator.failed.to.upload.course.title"), action = showLogAction)
      return
    }
    val courseBean = uploadCourseResponse.plugin
    course.id = courseBean.id
    YamlFormatSynchronizer.saveRemoteInfo(course)
    YamlFormatSynchronizer.saveItem(course)

    showInfoNotification(
      project,
      message("push.course.successfully.uploaded"),
      message("marketplace.push.course.successfully.uploaded.message", courseBean.name),
      openOnMarketplaceAction(course.getMarketplaceUrl())
    )
    LOG.info("Course ${courseBean.name} has been uploaded with id ${courseBean.id}")
  }

  private fun onAuthFailedActions(project: Project, failedActionTitle: String) {
    account = null
    showLoginNeededNotification(project, message("item.upload.to.0.course.title", MARKETPLACE), failedActionTitle) { doAuthorize() }
  }

  private fun <T> Call<T>.executeUploadParsingErrors(
    project: Project,
    failedActionTitle: String,
    onErrorAction: AnAction,
    showOnNotFoundCodeNotification: () -> Unit = {}
  ): Result<Response<T>, String> {
    val response = executeCall().onError {
      showErrorNotification(project, failedActionTitle, it)
      return Err(it)
    }
    val responseCode = response.code()
    val errorBody = response.errorBody()?.string() ?: return Ok(response)
    val errorMessage = "$errorBody Code $responseCode"
    val extractedErrorMessage = try {
      ((Json.parseToJsonElement(errorBody) as? JsonObject)?.get("message") as? JsonPrimitive)?.content
    }
    catch (e: SerializationException) {
      null
    }

    return when (responseCode) {
      HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> Ok(response)
      HttpURLConnection.HTTP_BAD_REQUEST -> {
        FailedCourseUploadResponse.parse(objectMapper, errorBody).errors.forEach {
          showErrorNotification(project, failedActionTitle, it)
        }
        Err(errorMessage) // 400
      }
      HttpURLConnection.HTTP_FORBIDDEN -> {
        showErrorNotification(project, failedActionTitle, extractedErrorMessage ?: errorMessage)
        onAuthFailedActions(project, failedActionTitle)
        Err(errorMessage) // 403
      }
      HttpURLConnection.HTTP_NOT_FOUND -> {
          showOnNotFoundCodeNotification()
          Err(errorMessage) //404
        }
      HttpURLConnection.HTTP_UNAVAILABLE, HttpURLConnection.HTTP_BAD_GATEWAY -> {
        showErrorNotification(project, failedActionTitle, action = onErrorAction)
        Err("${message("error.network.service.maintenance")}\n\n$errorMessage") // 502, 503
      }
      in HttpURLConnection.HTTP_INTERNAL_ERROR..HttpURLConnection.HTTP_VERSION -> {
        showErrorNotification(project, failedActionTitle, action = onErrorAction)
        Err("${message("error.network.service.down")}\n\n$errorMessage") // 500x
      }
      else -> {
        LOG.warn("Code $responseCode is not handled")
        showErrorNotification(project, failedActionTitle, action = onErrorAction)
        Err(message("error.network.unexpected.error", errorMessage))
      }
    }
  }

  private fun EduCourse.getMarketplaceUrl() = "$MARKETPLACE_PLUGIN_URL/${this.id}"

  private fun openOnMarketplaceAction(link: String): AnAction {
    return EduNotificationManager.openLinkAction(message("action.open.on.text", MARKETPLACE), link)
  }

  fun uploadCourseUpdateUnderProgress(project: Project, course: EduCourse, file: File, hubToken: String) {
    var courseVersionMismatch = false
    invokeAndWaitIfNeeded {
      uploadUnderProgress(message("push.course.updating.progress.title")) {
        courseVersionMismatch = uploadCourseUpdate(project, course, file, hubToken)
      }
    }
    if (courseVersionMismatch) {
      val updateActionTitle = message("item.update.on.0.course.title", MARKETPLACE)
      val insertedCourseVersion = createAndShowCourseVersionDialog(project, course, updateActionTitle)
                                  ?: return
      course.marketplaceCourseVersion = insertedCourseVersion
      YamlFormatSynchronizer.saveRemoteInfo(course)
      val pushAction = ActionManager.getInstance().getAction(MarketplacePushCourse.ACTION_ID)
      pushAction.templatePresentation.text = updateActionTitle
      showInfoNotification(project, message("notification.course.creator.inserted.course.version", insertedCourseVersion), action = pushAction)
    }
  }

  private fun uploadCourseUpdate(project: Project, course: EduCourse, file: File, hubToken: String): Boolean {
    LOG.info("Uploading course update from ${file.absolutePath}")
    val uploadAsNewCourseAction: AnAction = NotificationAction.createSimpleExpiring(
      message("notification.course.creator.access.denied.action")) {
      course.convertToLocal()
      uploadNewCourseUnderProgress(project, course, file, hubToken)
    }

    getRepositoryEndpoints(hubToken).uploadCourseUpdate(file.toMultipartBody(), course.id).executeUploadParsingErrors(project,
        message("notification.course.creator.failed.to.update.course.title"),
        uploadAsNewCourseAction,
        showOnNotFoundCodeNotification = {
          showFailedToFindMarketplaceCourseOnRemoteNotification(project, uploadAsNewCourseAction)
        }).onError {
        val message = "Failed to upload course update for course ${course.id}: $it"
        if (it.contains(PLUGIN_CONTAINS_VERSION_ERROR_TEXT)) {
          LOG.info(message)
          return true
        }
        LOG.error(message)
        return false
      }

    showInfoNotification(
      project,
      message("marketplace.push.course.successfully.updated.title"),
      message("marketplace.push.course.successfully.updated.message", course.name, course.marketplaceCourseVersion),
      action = openOnMarketplaceAction(course.getMarketplaceUrl())
    )
    LOG.info("Course ${course.name} update has been successfully uploaded with version ${course.marketplaceCourseVersion}")
    YamlFormatSynchronizer.saveItem(course)
    return false
  }

  /**
   * the following link formats are supported:
   * https://plugins.jetbrains.com/plugin/12345 link with numeric id
   * https://plugins.jetbrains.com/plugin/12345-plugin-name link with xmlId
   * 12345-plugin-name just a plugin xmlId
   */
  override fun getCourseIdFromLink(link: String): Int {
    if (link.matches(XML_ID)) {
      return parseXmlId(link)
    }
    try {
      val url = URL(link)
      val pathParts = url.path.split("/").dropLastWhile { it.isEmpty() }
      for (i in pathParts.indices) {
        val part = pathParts[i]
        if (part == "plugin" && i + 1 < pathParts.size) {
          val xmlId = pathParts[i + 1]
          return parseXmlId(xmlId)
        }
      }
    }
    catch (e: MalformedURLException) {
      LOG.warn(e.message)
    }

    return -1
  }

  override fun getCourseInfoByLink(link: String): EduCourse? {
    return getCourseInfoByLink(link, isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_PRIVATE_COURSES))
  }

  fun getCourseInfoByLink(link: String, searchPrivate: Boolean): EduCourse? {
    val courseId = link.toIntOrNull() ?: getCourseIdFromLink(link)
    if (courseId != -1) {
      // we don't know beforehand if the course to be searched for is private or public, while private and public courses
      // need different templates to be found via graphql
      return searchCourse(courseId, false) ?: if (searchPrivate) searchCourse(courseId, true) else null
    }
    return null
  }

  private fun parseXmlId(xmlId: String): Int {
    return try {
      Integer.parseInt(xmlId.split("-")[0])
    }
    catch (e: NumberFormatException) {
      -1
    }
  }

  fun isLoggedInAsync(): CompletableFuture<Boolean> {
    return CompletableFuture.supplyAsync({ isLoggedIn() }, ProcessIOExecutorService.INSTANCE)
  }

  companion object {
    private val LOG = logger<MarketplaceConnector>()

    private val XML_ID = "\\d{5,}-.*".toRegex()
    private const val PLUGIN_CONTAINS_VERSION_ERROR_TEXT = "plugin already contains version"

    fun getInstance(): MarketplaceConnector = service()
  }
}

private data class LoadedCourses(val courses: List<EduCourse>, val total: Int)