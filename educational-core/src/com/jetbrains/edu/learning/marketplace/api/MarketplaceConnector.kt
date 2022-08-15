package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showAcceptDeveloperAgreementNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showFailedToFindMarketplaceCourseOnRemoteNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLogAction
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLoginSuccessfulNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNoRightsToUpdateNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.coursecreator.actions.marketplace.MarketplacePushCourse
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.ConnectorUtils
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.authUtils.OAuthRestService.CODE_ARGUMENT
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.TOKEN_EXCHANGE
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.*
import com.jetbrains.edu.learning.marketplace.api.GraphqlQuery.LOADING_STEP
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.apache.http.client.utils.URIBuilder
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

abstract class MarketplaceConnector : EduOAuthConnector<MarketplaceAccount, MarketplaceUserInfo>(), CourseConnector {
  override var account: MarketplaceAccount?
    get() = MarketplaceSettings.INSTANCE.account
    set(account) {
      MarketplaceSettings.INSTANCE.account = account
    }

  override val authorizationUrl: String
    get() = URIBuilder(HUB_AUTH_URL)
      .setPath("$HUB_API_PATH/oauth2/auth")
      .addParameter("access_type", "offline")
      .addParameter("client_id", EDU_CLIENT_ID)
      .addParameter("redirect_uri", getRedirectUri())
      .addParameter("response_type", CODE_ARGUMENT)
      .addParameter("scope", "openid $EDU_CLIENT_ID $MARKETPLACE_CLIENT_ID")
      .build()
      .toString()

  override val clientId: String = EDU_CLIENT_ID

  override val clientSecret: String = EDU_CLIENT_SECRET

  override val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(EduCourse::class.java, MarketplaceEduCourseMixin::class.java)
    objectMapper
  }

  override val platformName: String = MARKETPLACE

  protected abstract val repositoryUrl: String

  private val marketplaceEndpoints: MarketplaceEndpoints
    get() = getEndpoints()

  private val repositoryEndpoints: MarketplaceRepositoryEndpoints
    get() = getEndpoints(baseUrl = repositoryUrl)

  private val extensionGrantsEndpoint: MarketplaceExtensionGrantsEndpoints
    get() = getEndpoints(baseUrl = JB_ACCOUNT_URL)


  // Authorization requests:

  @Synchronized
  override fun login(code: String): Boolean {
    val hubTokenInfo = retrieveLoginToken(code, getRedirectUri()) ?: return false
    val account = MarketplaceAccount(hubTokenInfo.expiresIn)
    val currentUser = getUserInfo(account, hubTokenInfo.accessToken) ?: return false
    if (currentUser.isGuest) {
      // it means that session is broken, so we should force user to re-login
      LOG.warn("User ${currentUser.name} is anonymous")
      this.account = null
      return false
    }
    account.userInfo = currentUser

    // Hub token and JBA token both have expiresIn times, which are of the same duration.
    // We keep the hub token expiresIn interval as the shortest one.
    val jBAccountTokenInfo = if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS)) {
      retrieveJBAccountToken(hubTokenInfo.idToken)
    }
    else TokenInfo()

    if (jBAccountTokenInfo == null) {
      LOG.error("Failed to obtain JBA token via extension grants")
      return false
    }

    this.account = account
    account.saveTokens(hubTokenInfo)
    account.saveJBAccountToken(jBAccountTokenInfo.idToken)
    return true
  }

  private fun retrieveJBAccountToken(hubIdToken: String?): TokenInfo? {
    if (hubIdToken.isNullOrEmpty()) {
      LOG.error("Failed to obtain JB account token via extension grants. Hub id token is null")
      return null
    }
    val response = extensionGrantsEndpoint.exchangeTokens(TOKEN_EXCHANGE, hubIdToken).executeHandlingExceptions()
    return response?.body()
  }

  override fun refreshTokens() {
    super.refreshTokens()

    if (!isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS)) return
    val currentAccount = account ?: error("No logged in user")
    val jBAccountToken = retrieveJBAccountToken(currentAccount.getHubIdToken()) ?: error(
      "Failed to obtain JB account token via extension grants at token refresh")
    currentAccount.saveJBAccountToken(jBAccountToken.idToken)
  }

  override fun getNewTokens(): TokenInfo {
    val currentAccount = account ?: error("No logged in user")
    val tokens = super.getNewTokens()
    if (tokens.refreshToken.isEmpty()) {
      // hub documentation https://www.jetbrains.com/help/hub/Refresh-Token.html#AccessTokenRequestError
      // says that new refresh token may be issued by hub, but that's not obligatory,
      // old refresh token should be used in this case
      tokens.refreshToken = currentAccount.getRefreshToken() ?: error("Refresh token is null")
    }
    return tokens
  }

  // Get requests:

  /**
   * For getting user info from Marketplace, account and access token must not to be passed to
   * [com.jetbrains.edu.learning.api.EduOAuthConnector.getEndpoints]
   */
  override fun getUserInfo(account: MarketplaceAccount, accessToken: String?): MarketplaceUserInfo? {
    val token = accessToken ?: return null
    val userId = decodeHubToken(token) ?: return null
    val response = marketplaceEndpoints.getUserInfo(userId).executeHandlingExceptions()
    return response?.body()
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
      val coursesInfoList = getCoursesInfoList(offset, searchPrivate) ?: return courses
      val loadedCourses = updateFormatVersions(coursesInfoList)
      if (loadedCourses.isNullOrEmpty()) return courses
      courses.addAll(loadedCourses)
      offset += LOADING_STEP
    }
    while (courses.size < coursesInfoList.total)

    return courses
  }

  private fun updateFormatVersions(coursesInfoList: CoursesInfoList): List<EduCourse>? {
    val courses = coursesInfoList.courses.toMutableList()
    val updates = getUpdateInfoList(courses.map { it.id }) ?: return null
    val courseIdJsonVersionMap: Map<Int, Int> = updates.associateBy(UpdateInfo::pluginId) { it.compatibility.gte }
    for (course in courses) {
      val courseFormatVersion = courseIdJsonVersionMap[course.id]
      if (courseFormatVersion == null) {
        courses.remove(course)
        LOG.error("No UpdateInfo found for course ${course.name}")
        continue
      }
      course.formatVersion = courseFormatVersion
    }
    return courses
  }

  private fun getCoursesInfoList(offset: Int, searchPrivate: Boolean): CoursesInfoList? {
    val query = QueryData(GraphqlQuery.search(offset, searchPrivate))
    val response = repositoryEndpoints.search(query).executeHandlingExceptions()
    return response?.body()?.data?.myCoursesInfoList
  }

  fun searchCourse(courseId: Int, searchPrivate: Boolean = false): EduCourse? {
    val query = QueryData(GraphqlQuery.searchById(courseId, searchPrivate))
    val response = repositoryEndpoints.search(query).executeHandlingExceptions()
    val coursesInfoList = response?.body()?.data?.myCoursesInfoList ?: return null
    val course = updateFormatVersions(coursesInfoList)?.firstOrNull()
    course?.id = courseId
    return course
  }

  fun getLatestCourseUpdateInfo(courseId: Int): UpdateInfo? {
    val updateInfoList = getUpdateInfoList(listOf(courseId))
    if (updateInfoList == null) {
      error("Update info list for course $courseId is null")
    }
    else {
      return updateInfoList.firstOrNull()
    }
  }

  private fun getUpdateInfoList(courseIds: List<Int>): List<UpdateInfo>? {
    val response = repositoryEndpoints.getUpdateId(QueryData(GraphqlQuery.lastUpdatesList(courseIds))).executeHandlingExceptions()
    return response?.body()?.data?.updates?.updateInfoList
  }

  @Suppress("UnstableApiUsage")
  fun loadCourseStructure(course: EduCourse) {
    val buildNumber = ApplicationInfoImpl.getShadowInstanceImpl().pluginsCompatibleBuild

    val uuid = PluginDownloader.getMarketplaceDownloadsUUID()

    val link = "$repositoryUrl/plugin/download?updateId=${course.getLatestUpdateId()}&uuid=$uuid&build=$buildNumber"
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

    val response = repositoryEndpoints.uploadNewCourse(file.toMultipartBody(), LICENSE_URL.toRequestBody())
      .executeUploadParsingErrors(project,
                                  message("notification.course.creator.failed.to.upload.course.title"),
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

  private fun <T> Call<T>.executeUploadParsingErrors(project: Project,
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
      HttpURLConnection.HTTP_BAD_REQUEST -> {
        Err(errorMessage) // 400
      }
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
    var courseVersionMismatch = false
    invokeAndWaitIfNeeded {
      uploadUnderProgress(message("push.course.updating.progress.title")) {
        courseVersionMismatch = uploadCourseUpdate(project, course, file)
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
      showNotification(project, message("marketplace.inserted.course.version.notification", insertedCourseVersion), pushAction)
    }
  }

  private fun uploadCourseUpdate(project: Project, course: EduCourse, file: File): Boolean {
    if (!isUserAuthorized()) return false
    LOG.info("Uploading course update from ${file.absolutePath}")
    val uploadAsNewCourseAction: AnAction = NotificationAction.createSimpleExpiring(
      message("notification.course.creator.access.denied.action")) {
      course.convertToLocal()
      uploadNewCourseUnderProgress(project, course, file)
    }

    repositoryEndpoints.uploadCourseUpdate(file.toMultipartBody(), course.id)
      .executeUploadParsingErrors(project,
                                  message("notification.course.creator.failed.to.update.course.title"),
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
        val message = "Failed to upload course update for course ${course.id}: ${it}"
        if (it.contains(PLUGIN_CONTAINS_VERSION_ERROR_TEXT)) {
          LOG.info(message)
          return true
        }
        LOG.error(message)
        return false
      }

    val message = message("marketplace.push.course.successfully.updated", course.name, course.marketplaceCourseVersion)
    showNotification(project, message, openOnMarketplaceAction(course.getMarketplaceUrl()))
    LOG.info(message)
    YamlFormatSynchronizer.saveItem(course)
    return false
  }

  private fun isUserAuthorized(): Boolean {
    if (account == null) {
      // we check that user isn't null before `postCourse` call
      LOG.warn("User is null when posting the course")
      return false
    }
    return true
  }

  override fun setPostLoginActions(postLoginActions: List<Runnable>) {
    val requestFocus = Runnable { runInEdt { requestFocus() } }
    val showNotification = Runnable {
      val userName = account?.userInfo?.getFullName() ?: return@Runnable
      showLoginSuccessfulNotification(userName)
    }
    val actions = postLoginActions + listOf(requestFocus, showNotification)
    super.setPostLoginActions(actions)
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


  @Suppress("DialogTitleCapitalization")
  private fun createAndShowCourseVersionDialog(project: Project, course: EduCourse, failedActionTitle: String): Int? {
    val currentCourseVersion = course.marketplaceCourseVersion
    val suggestedCourseVersion = currentCourseVersion + 1

    return invokeAndWaitIfNeeded {
      Messages.showInputDialog(project,
                               message("marketplace.insert.course.version.dialog", currentCourseVersion, course.name, failedActionTitle),
                               message("marketplace.insert.course.version.dialog.title"),
                               null,
                               suggestedCourseVersion.toString(),
                               NumericInputValidator(message("marketplace.insert.course.version.validation.empty"),
                                                     message("marketplace.insert.course.version.validation.not.numeric")))?.toIntOrNull()
    }
  }

  companion object {
    private val LOG = logger<MarketplaceConnector>()

    private val MARKETPLACE_CLIENT_ID: String = MarketplaceOAuthBundle.value("marketplaceHubClientId")
    private val EDU_CLIENT_ID: String = MarketplaceOAuthBundle.value("eduHubClientId")
    private val EDU_CLIENT_SECRET: String = MarketplaceOAuthBundle.value("eduHubClientSecret")

    private val XML_ID = "\\d{5,}-.*".toRegex()
    private const val PLUGIN_CONTAINS_VERSION_ERROR_TEXT = "plugin already contains version"

    @JvmStatic
    fun getInstance(): MarketplaceConnector = service()
  }
}