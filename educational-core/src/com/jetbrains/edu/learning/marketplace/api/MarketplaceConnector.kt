package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.OAuthUtils.checkBuiltinPortValid
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.*
import com.jetbrains.edu.learning.marketplace.CLIENT_ID
import com.jetbrains.edu.learning.marketplace.CLIENT_SECRET
import com.jetbrains.edu.learning.marketplace.HUB_AUTHORISATION_CODE_URL
import com.jetbrains.edu.learning.marketplace.REDIRECT_URI
import com.jetbrains.edu.learning.marketplace.api.Graphql.Companion.LOADING_STEP
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory

abstract class MarketplaceConnector {
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
    if (!isUnitTestMode && account != null && !account.tokenInfo.isUpToDate()) {
      account.refreshTokens()
    }

    val retrofit = createRetrofitBuilder(repositoryUrl, connectionPool, accessToken = account?.tokenInfo?.accessToken)
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
    val response = authorizationService.getTokens(CLIENT_ID,
                                                  CLIENT_SECRET,
                                                  REDIRECT_URI, code,
                                                  AUTHORIZATION_CODE).executeHandlingExceptions()
    val tokenInfo = response?.body() ?: return false
    val userId = decodeHubToken(tokenInfo.accessToken) ?: return false
    val account = MarketplaceAccount()
    account.tokenInfo = tokenInfo
    val currentUser = getCurrentUser(userId) ?: return false
    account.userInfo = currentUser
    MarketplaceSettings.INSTANCE.account = account
    ApplicationManager.getApplication().messageBus.syncPublisher(AUTHORIZATION_TOPIC).userLoggedIn()
    return true
  }

  private fun MarketplaceAccount.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val response = authorizationService.refreshTokens(REFRESH_TOKEN,
                                                      CLIENT_ID,
                                                      CLIENT_SECRET,
                                                      refreshToken).executeHandlingExceptions()
    val tokens = response?.body() ?: return
    // hub documentation https://www.jetbrains.com/help/hub/Refresh-Token.html#AccessTokenRequestError
    // says that new refresh token may be issued by hub, but that's not obligatory,
    // old refresh token should be used in this case
    if (tokens.refreshToken.isEmpty()) {
      tokens.refreshToken = refreshToken
    }
    updateTokens(tokens)
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
    var offset = 0
    val courses = mutableListOf<EduCourse>()

    do {
      val coursesList = getCourses(offset) ?: return courses
      val loadedCourses = coursesList.courses
      if (loadedCourses.isEmpty()) return courses
      courses.addAll(loadedCourses)
      offset += LOADING_STEP
    }
    while (courses.size < coursesList.total)

    return courses.toList()
  }

  private fun getCourses(offset: Int): CoursesList? {
    val query = QueryData(Graphql().getSearchQuery(offset))
    val response = repositoryService.search(query).executeHandlingExceptions()
    return response?.body()?.data?.coursesList
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
      }
    })
  }

  private fun createMapper(module: SimpleModule): ObjectMapper {
    val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    objectMapper.addMixIn(EduCourse::class.java, MarketplaceEduCourseMixin::class.java)
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    objectMapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_SETTERS)
    objectMapper.registerModule(module)
    return objectMapper
  }

  companion object {
    private val LOG = logger<MarketplaceConnector>()

    @JvmStatic
    val AUTHORIZATION_TOPIC = Topic.create("Edu.marketplaceLoggedIn", EduLogInListener::class.java)

    @JvmStatic
    fun getInstance(): MarketplaceConnector = service()
  }
}