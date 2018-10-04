package com.jetbrains.edu.learning.stepik.alt

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.courseFormat.Lesson
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.jetbrains.ide.BuiltInServerManager
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

object HyperskillConnector {
  private const val HYPERSKILL_URL = "https://hyperskill.org/"
  private val port = BuiltInServerManager.getInstance().port
  private val redirectUri = "http://localhost:$port/api/edu/hyperskill/oauth"

  private var clientId = "jcboczaSZYHmmCewusCNrE172yHkOONV7JY1ECh4"
  private val authorizationCodeUrl = "https://hyperskill.org/oauth2/authorize/?" +
                                     "client_id=$clientId&redirect_uri=$redirectUri&grant_type=code&scope=read+write&response_type=code"
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
    try {
      createAuthorizationListener(*postLoginActions)
      BrowserUtil.browse(authorizationCodeUrl)
    }
    catch (e: URISyntaxException) {
      // IOException is thrown when there're no available ports, in some cases restarting can fix this
      //TODO: handle exceptions
    }
    catch (e: IOException) {
      //TODO:
    }
  }

  fun login(code: String): Boolean {
    val tokenInfo = service.getTokens(clientId, redirectUri, code, "authorization_code").execute().body() ?: return false
    HyperskillSettings.instance.account = HyperskillAccount(tokenInfo)
    val currentUser = getCurrentUser() ?: return false
    HyperskillSettings.instance.account!!.userInfo = currentUser
    ApplicationManager.getApplication().messageBus.syncPublisher<HyperskillLoggedIn>(authorizationTopic).userLoggedIn()
    return true
  }

  private fun getCurrentUser(): HyperskillUserInfo? {
    return service.getUserInfo(0).execute().body()?.users?.first() ?: return null
  }

  fun getLessons(): List<Lesson> {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<List<Lesson>, Exception>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        val userInfo = HyperskillSettings.instance.account?.userInfo ?: return@runProcessWithProgressSynchronously emptyList()
        val topics = service.topics(stage = userInfo.stage?.id.toString()).execute().body()?.topics ?:
                     return@runProcessWithProgressSynchronously emptyList()

        val lessonIds = mutableListOf<Int>()
        for (topic in topics.filter { it.children.isEmpty()}) {
          if (topic.hasLessons) {
            val lessonsIds = service.lessons(topic.id).execute().body()?.lessons?.map { it.stepikId } ?: continue
            lessonIds.addAll(lessonsIds)
          }
        }
        return@runProcessWithProgressSynchronously getLessons(lessonIds.map { it -> it.toString() })
      }, "Loading course", false, null)
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

  @FunctionalInterface
  private interface HyperskillLoggedIn {
    fun userLoggedIn()
  }
}
