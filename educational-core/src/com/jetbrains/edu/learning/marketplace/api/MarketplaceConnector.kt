package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.intellij.util.messages.Topic
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showAcceptDeveloperAgreementNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showFailedToFindMarketplaceCourseOnRemoteNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLogAction
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLoginSuccessfulNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNoRightsToUpdateNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.OAuthUtils.checkBuiltinPortValid
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.*
import com.jetbrains.edu.learning.marketplace.api.GraphqlQuery.LOADING_STEP
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import okhttp3.ConnectionPool
import retrofit2.Call
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

abstract class MarketplaceConnector : CourseConnector {
  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper

  protected abstract val authUrl: String

  protected abstract val repositoryUrl: String

  init {
    val module = SimpleModule()
    objectMapper = createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private val authorizationService: MarketplaceAuthService
    get() {
      val retrofit = createRetrofitBuilder(authUrl, connectionPool)
        .addConverterFactory(converterFactory)
        .build()

      return retrofit.create(MarketplaceAuthService::class.java)
    }

  private val repositoryService: MarketplaceRepositoryService
    get() = repositoryService(MarketplaceSettings.INSTANCE.account)

  private fun repositoryService(account: MarketplaceAccount?): MarketplaceRepositoryService {
    if (!isUnitTestMode && account != null && !account.isUpToDate()) {
      account.refreshTokens()
    }

    val accessToken = account?.getAccessToken()
    val retrofit = createRetrofitBuilder(repositoryUrl, connectionPool, accessToken = accessToken)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(MarketplaceRepositoryService::class.java)
  }

  // Authorization requests:

  fun doAuthorize(vararg postLoginActions: Runnable) {
    if (!checkBuiltinPortValid()) return

    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(HUB_AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val response = authorizationService.getTokens(EDU_CLIENT_ID,
                                                  EDU_CLIENT_SECRET,
                                                  REDIRECT_URI, code,
                                                  AUTHORIZATION_CODE).executeHandlingExceptions()
    val tokenInfo = response?.body() ?: return false
    val userId = decodeHubToken(tokenInfo.accessToken) ?: return false
    val account = MarketplaceAccount(tokenInfo.expiresIn)
    val currentUser = getCurrentUser(userId) ?: return false
    account.userInfo = currentUser
    MarketplaceSettings.INSTANCE.account = account
    account.saveTokens(tokenInfo)
    ApplicationManager.getApplication().messageBus.syncPublisher(AUTHORIZATION_TOPIC).userLoggedIn()
    return true
  }

  private fun MarketplaceAccount.refreshTokens() {
    val refreshToken = getRefreshToken() ?: error("Refresh token is null")
    val response = authorizationService.refreshTokens(REFRESH_TOKEN,
                                                      EDU_CLIENT_ID,
                                                      EDU_CLIENT_SECRET,
                                                      refreshToken).executeHandlingExceptions()
    val tokens = response?.body() ?: return
    // hub documentation https://www.jetbrains.com/help/hub/Refresh-Token.html#AccessTokenRequestError
    // says that new refresh token may be issued by hub, but that's not obligatory,
    // old refresh token should be used in this case
    if (tokens.refreshToken.isEmpty()) {
      tokens.refreshToken = refreshToken
    }
    tokenExpiresIn = tokens.expiresIn
    saveTokens(tokens)
  }

  // Get requests:

  private fun getCurrentUser(userId: String): MarketplaceUserInfo? {
    val response = authorizationService.getCurrentUserInfo(userId).executeHandlingExceptions()
    val userInfo = response?.body() ?: return null
    if (userInfo.guest) {
      // it means that session is broken and we should force user to relogin
      LOG.warn("User ${userInfo.name} is anonymous")
      MarketplaceSettings.INSTANCE.account = null
      return null
    }
    return userInfo
  }

  fun searchCourses(): List<EduCourse> {
    val allCourses = mutableListOf<EduCourse>()
    allCourses.addAll(searchCourses(false))
    if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_PRIVATE_COURSES)) {
      allCourses.addAll(searchCourses(true))
    }
    return allCourses
  }

  fun searchCourses(searchPrivate: Boolean): List<EduCourse> {
    var offset = 0
    val courses = mutableListOf<EduCourse>()

    do {
      val coursesList = getCourses(offset, searchPrivate) ?: return courses
      val loadedCourses = coursesList.courses
      if (loadedCourses.isEmpty()) return courses
      courses.addAll(loadedCourses)
      offset += LOADING_STEP
    }
    while (courses.size < coursesList.total)

    return courses
  }

  private fun getCourses(offset: Int, searchPrivate: Boolean): CoursesList? {
    val query = QueryData(GraphqlQuery.search(offset, searchPrivate))
    val response = repositoryService.search(query).executeHandlingExceptions()
    return response?.body()?.data?.coursesList
  }

  fun searchCourse(courseId: Int, searchPrivate: Boolean = false): EduCourse? {
    val query = QueryData(GraphqlQuery.searchById(courseId, searchPrivate))
    val response = repositoryService.search(query).executeHandlingExceptions()
    val course = response?.body()?.data?.coursesList?.courses?.firstOrNull()
    course?.id = courseId
    return course
  }

  fun getLatestCourseUpdateInfo(courseId: Int): UpdateInfo? {
    val response = repositoryService.getUpdateId(QueryData(GraphqlQuery.lastUpdateId(courseId))).executeHandlingExceptions()
    val updateInfoList = response?.body()?.data?.updates?.updateInfoList
    if (updateInfoList == null) {
      error("Update info list for course $courseId is null")
    }
    else {
      return updateInfoList.firstOrNull()
    }
  }

  @Suppress("UnstableApiUsage")
  fun loadCourseStructure(course: EduCourse) {
    val buildNumber = getBuildNumberForRequests()

    //BACKCOMPAT 221: replace with com.intellij.openapi.updateSettings.impl.PluginDownloader.getMarketplaceDownloadsUUID()
    val uuid = UUIDProvider.getUUID()

    val link = "$repositoryUrl/plugin/${course.id}/update/${course.getLatestUpdateId()}/download?uuid=$uuid&build=$buildNumber"
    val tempFile = FileUtil.createTempFile("marketplace-${course.name}", ".zip", true)
    DownloadUtil.downloadAtomically(null, link, tempFile)

    val unpackedCourse = EduUtils.getLocalCourse(tempFile.path) as? EduCourse ?: error(
      message("dialog.title.failed.to.unpack.course"))

    course.items = unpackedCourse.items
    course.additionalFiles = unpackedCourse.additionalFiles
  }

  private fun EduCourse.getLatestUpdateId(): Int {
    val updateInfo = getLatestCourseUpdateInfo(id)
    if (updateInfo == null) {
      error("Update info for course $id is null")
    }
    marketplaceCourseVersion = updateInfo.version
    return updateInfo.updateId
  }

  private fun uploadUnderProgress(message: String, uploadAction: () -> Unit) =
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable {
          uploadAction()
        }
      }, message, true, null)

  fun uploadNewCourseUnderProgress(project: Project, course: EduCourse, file: File) {
    uploadUnderProgress(message("action.push.course")) {
      uploadNewCourse(project, course, file)
    }
  }

  private fun uploadNewCourse(project: Project, course: EduCourse, file: File) {
    if (!isUserAuthorized()) return
    LOG.info("Uploading new course from ${file.absolutePath}")

    val response = repositoryService.uploadNewCourse(file.toMultipartBody(), LICENSE_URL.toRequestBody())
      .executeUploadParsingErrors(project, message("notification.course.creator.failed.to.upload.course.title"),
                                  showLogAction,
                                  {
                                    showAcceptDeveloperAgreementNotification(project) { openOnMarketplaceAction(MARKETPLACE_PROFILE_PATH) }
                                  }, {})
      .onError {
        LOG.error("Failed to upload course ${course.name}: $it")
        return
      }

    val courseBean = response.body()
    if (courseBean == null) {
      showErrorNotification(project, message("notification.course.creator.failed.to.upload.course.title"), action = showLogAction)
      return
    }
    course.id = courseBean.id
    YamlFormatSynchronizer.saveRemoteInfo(course)
    YamlFormatSynchronizer.saveItem(course)

    val message = message("marketplace.push.course.successfully.uploaded", courseBean.name)
    showNotification(project,
                     openOnMarketplaceAction(course.getMarketplaceUrl()),
                     message,
                     message("marketplace.push.course.successfully.uploaded.message"))
    LOG.info("$message with id ${courseBean.id}")
  }

  fun <T> Call<T>.executeUploadParsingErrors(project: Project,
                                             failedActionMessage: String,
                                             onErrorAction: AnAction,
                                             showOnForbiddenCodeNotification: () -> Unit,
                                             showOnNotFoundCodeNotification: () -> Unit
  ): Result<Response<T>, String> {
    val response = executeCall().onError { return Err(it) }
    val responseCode = response.code()
    val errorBody = response.errorBody()?.string() ?: return Ok(response)
    val errorMessage = "$errorBody Code $responseCode"
    return when (responseCode) {
      HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> Ok(response)
      HttpURLConnection.HTTP_FORBIDDEN -> {
        showOnForbiddenCodeNotification()
        Err(errorMessage) // 403
      }
      HttpURLConnection.HTTP_NOT_FOUND -> {
          showOnNotFoundCodeNotification()
          Err(errorMessage) //404
        }
      HttpURLConnection.HTTP_UNAVAILABLE, HttpURLConnection.HTTP_BAD_GATEWAY -> {
        showErrorNotification(project, failedActionMessage, action = onErrorAction)
        Err("${message("error.service.maintenance")}\n\n$errorMessage") // 502, 503
      }
      in HttpURLConnection.HTTP_INTERNAL_ERROR..HttpURLConnection.HTTP_VERSION -> {
        showErrorNotification(project, failedActionMessage, action = onErrorAction)
        Err("${message("error.service.down")}\n\n$errorMessage") // 500x
      }
      else -> {
        LOG.warn("Code $responseCode is not handled")
        showErrorNotification(project, failedActionMessage, action = onErrorAction)
        Err(message("error.unexpected.error", errorMessage))
      }
    }
  }

  private fun EduCourse.getMarketplaceUrl() = "$MARKETPLACE_PLUGIN_URL/${this.id}"

  private fun openOnMarketplaceAction(link: String): AnAction {
    return object : AnAction(message("action.open.on.text", MARKETPLACE)) {
      override fun actionPerformed(e: AnActionEvent) {
        EduBrowser.getInstance().browse(link)
      }
    }
  }

  fun uploadCourseUpdateUnderProgress(project: Project, course: EduCourse, file: File) {
    uploadUnderProgress(message("action.push.course.updating")) { uploadCourseUpdate(project, course, file) }
  }

  private fun uploadCourseUpdate(project: Project, course: EduCourse, file: File) {
    if (!isUserAuthorized()) return
    LOG.info("Uploading course update from ${file.absolutePath}")
    val uploadAsNewCourseAction: AnAction = NotificationAction.createSimpleExpiring(
      EduCoreBundle.message("notification.course.creator.access.denied.action")) {
      course.convertToLocal()
      uploadNewCourseUnderProgress(project, course, file)
    }

    repositoryService.uploadCourseUpdate(file.toMultipartBody(), course.id)
      .executeUploadParsingErrors(project, message("notification.course.creator.failed.to.update.course.title"),
                                  uploadAsNewCourseAction,
                                  {
                                    showNoRightsToUpdateNotification(project, course) {
                                      uploadNewCourseUnderProgress(project, course, file)
                                    }
                                  },
                                  {
                                    showFailedToFindMarketplaceCourseOnRemoteNotification(project, uploadAsNewCourseAction)
                                  })
      .onError {
        LOG.error("Failed to upload course update for course ${course.id}: ${it}")
        return
      }

    val message = message("marketplace.push.course.successfully.updated", course.name, course.marketplaceCourseVersion)
    showNotification(project, message, openOnMarketplaceAction(course.getMarketplaceUrl()))
    LOG.info(message)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun isUserAuthorized(): Boolean {
    val user = MarketplaceSettings.INSTANCE.account
    if (user == null) {
      // we check that user isn't null before `postCourse` call
      LOG.warn("User is null when posting the course")
      return false
    }
    return true
  }

  private fun createAuthorizationListener(vararg postLoginActions: Runnable) {
    authorizationBusConnection.disconnect()
    authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
    authorizationBusConnection.subscribe(AUTHORIZATION_TOPIC, object : EduLogInListener {
      override fun userLoggedOut() {}

      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
        runInEdt { requestFocus() }
        val userName = MarketplaceSettings.INSTANCE.account?.userInfo?.getFullName() ?: return
        showLoginSuccessfulNotification(userName)
      }
    })
  }

  private fun createMapper(module: SimpleModule): ObjectMapper {
    val objectMapper = MarketplaceSubmissionsConnector.createMapper(module)
    objectMapper.addMixIn(EduCourse::class.java, MarketplaceEduCourseMixin::class.java)
    return objectMapper
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
    val courseId = link.toIntOrNull() ?: getCourseIdFromLink(link)
    if (courseId != -1) {
      // we don't know beforehand if the course to be searched for is private or public, while private and public courses
      // need different templates to be found via graphql
      return searchCourse(courseId, false) ?: if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_PRIVATE_COURSES))
        searchCourse(courseId, true)
      else null
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

  companion object {
    private val LOG = logger<MarketplaceConnector>()

    private val XML_ID = "\\d{5,}-.*".toRegex()

    @JvmStatic
    val AUTHORIZATION_TOPIC = Topic.create("Edu.marketplaceLoggedIn", EduLogInListener::class.java)

    @JvmStatic
    fun getInstance(): MarketplaceConnector = service()
  }
}