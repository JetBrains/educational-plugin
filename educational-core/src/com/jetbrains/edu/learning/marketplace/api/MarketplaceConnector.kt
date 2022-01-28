package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.intellij.util.io.URLUtil
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
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.TOKEN_EXCHANGE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.checkBuiltinPortValid
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.*
import com.jetbrains.edu.learning.marketplace.api.GraphqlQuery.LOADING_STEP
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

abstract class MarketplaceConnector : EduOAuthConnector<MarketplaceAccount, MarketplaceUserInfo>(), CourseConnector {
  override val account: MarketplaceAccount?
    get() = MarketplaceSettings.INSTANCE.account

  override val authorizationTopicName: String = "Edu.marketplaceLoggedIn"

  override val clientId: String = EDU_CLIENT_ID

  override val clientSecret: String = EDU_CLIENT_SECRET

  override val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(EduCourse::class.java, MarketplaceEduCourseMixin::class.java)
    objectMapper
  }

  protected abstract val repositoryUrl: String

  private val marketplaceEndpoints: MarketplaceEndpoints
    get() = getEndpoints()

  private val repositoryEndpoints: MarketplaceRepositoryEndpoints
    get() = getEndpoints(baseUrl = repositoryUrl)

  private val extensionGrantsEndpoint: MarketplaceExtensionGrantsEndpoints
    get() = getEndpoints(baseUrl = JB_ACCOUNT_URL)


  // Authorization requests:

  fun doAuthorize(vararg postLoginActions: Runnable) {
    if (!checkBuiltinPortValid()) return

    initiateAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(HUB_AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val hubTokenInfo = retrieveLoginToken(code, REDIRECT_URI) ?: return false
    val account = MarketplaceAccount(hubTokenInfo.expiresIn)
    val currentUser = getUserInfo(account, hubTokenInfo.accessToken) ?: return false
    if (currentUser.isGuest) {
      // it means that session is broken, so we should force user to re-login
      LOG.warn("User ${currentUser.name} is anonymous")
      MarketplaceSettings.INSTANCE.account = null
      return false
    }
    account.userInfo = currentUser

    // Hub token and JBA token both have expiresIn times, which are of the same duration.
    // We keep the hub token expiresIn interval as the shortest one.
    val jBAccountTokenInfo = retrieveJBAccountToken(hubTokenInfo.idToken)
    if (jBAccountTokenInfo == null) {
      LOG.error("Failed to obtain JBA token via extension grants")
      return false
    }

    MarketplaceSettings.INSTANCE.account = account
    account.saveTokens(hubTokenInfo)
    account.saveJBAccountToken(jBAccountTokenInfo.idToken)
    notifyUserLoggedIn()
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
    val response = repositoryEndpoints.search(query).executeHandlingExceptions()
    return response?.body()?.data?.coursesList
  }

  fun searchCourse(courseId: Int, searchPrivate: Boolean = false): EduCourse? {
    val query = QueryData(GraphqlQuery.searchById(courseId, searchPrivate))
    val response = repositoryEndpoints.search(query).executeHandlingExceptions()
    val course = response?.body()?.data?.coursesList?.courses?.firstOrNull()
    course?.id = courseId
    return course
  }

  fun getLatestCourseUpdateInfo(courseId: Int): UpdateInfo? {
    val response = repositoryEndpoints.getUpdateId(QueryData(GraphqlQuery.lastUpdateId(courseId))).executeHandlingExceptions()
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
      uploadUnderProgress(message("action.push.course.updating")) {
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

  private fun initiateAuthorizationListener(vararg postLoginActions: Runnable) =
    reconnectAndSubscribe(object : EduLogInListener {
      override fun userLoggedOut() {}

      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
        runInEdt { requestFocus() }
        val userName = account?.userInfo?.getFullName() ?: return
        showLoginSuccessfulNotification(userName)
      }
    })

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
  fun createAndShowCourseVersionDialog(project: Project, course: EduCourse, failedActionTitle: String): Int? {
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
    private val HUB_AUTHORISATION_CODE_URL: String
      get() = "${HUB_AUTH_URL}oauth2/auth?" +
              "response_type=code&redirect_uri=${URLUtil.encodeURIComponent(REDIRECT_URI)}&" +
              "client_id=$EDU_CLIENT_ID&scope=openid%20$EDU_CLIENT_ID%20$MARKETPLACE_CLIENT_ID&access_type=offline"
    private val XML_ID = "\\d{5,}-.*".toRegex()
    private const val PLUGIN_CONTAINS_VERSION_ERROR_TEXT = "plugin already contains version"

    @JvmStatic
    fun getInstance(): MarketplaceConnector = service()
  }
}