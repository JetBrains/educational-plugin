package com.jetbrains.edu.learning.stepik.alt

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

object HyperskillConnector {
  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
  private val authorizationTopic = com.intellij.util.messages.Topic.create<HyperskillLoggedIn>("Edu.hyperskillLoggedIn",
                                                                                               HyperskillLoggedIn::class.java)

  private val service: HyperskillService
    get() {
      val dispatcher = Dispatcher()
      dispatcher.maxRequests = 10

      val okHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
          val tokenInfo = HyperskillSettings.instance.account?.tokenInfo
          if (tokenInfo == null) return@addInterceptor chain.proceed(chain.request())

          val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${tokenInfo.accessToken}")
            .build()
          chain.proceed(newRequest)
        }
        .dispatcher(dispatcher)
        .build()
      val retrofit = Retrofit.Builder()
        .baseUrl(HYPERSKILL_URL)
        .addConverterFactory(JacksonConverterFactory.create())
        .client(okHttpClient)
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  fun doAuthorize(vararg postLoginActions: Runnable) {
    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val tokenInfo = service.getTokens(CLIENT_ID, REDIRECT_URI, code, "authorization_code").execute().body() ?: return false
    HyperskillSettings.instance.account = HyperskillAccount()
    HyperskillSettings.instance.account!!.tokenInfo = tokenInfo
    val currentUser = getCurrentUser() ?: return false
    HyperskillSettings.instance.account!!.userInfo = currentUser
    ApplicationManager.getApplication().messageBus.syncPublisher<HyperskillLoggedIn>(authorizationTopic).userLoggedIn()
    return true
  }

  private fun getCurrentUser(): HyperskillUserInfo? {
    return service.getUserInfo(0).execute().body()?.users?.first() ?: return null
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    return service.stages(projectId).execute().body()?.stages
  }

  fun getProjects(): List<HyperskillProject>? {
    return service.projects().execute().body()?.projects
  }

  private fun createAuthorizationListener(vararg postLoginActions: Runnable) {
    authorizationBusConnection.disconnect()
    authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
    authorizationBusConnection.subscribe(authorizationTopic, object : HyperskillLoggedIn {
      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
      }
    })
  }

  private interface HyperskillLoggedIn {
    fun userLoggedIn()
  }
}
