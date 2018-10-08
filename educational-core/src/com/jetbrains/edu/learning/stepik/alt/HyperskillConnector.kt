package com.jetbrains.edu.learning.stepik.alt

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ConcurrencyUtil
import com.jetbrains.edu.learning.courseFormat.Section
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object HyperskillConnector {
  private val THREAD_NUMBER = Runtime.getRuntime().availableProcessors()
  private val EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_NUMBER)

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

  fun getSections(languageId: String): List<Section> {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<List<Section>, Exception>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        ProgressManager.getInstance().progressIndicator.text2 = "Loading course structure"
        val userInfo = HyperskillSettings.instance.account?.userInfo ?: return@runProcessWithProgressSynchronously emptyList()
        val topics = service.topics(stage = userInfo.stage?.id.toString()).execute().body()?.topics?.filter { it.children.isEmpty()} ?:
                     return@runProcessWithProgressSynchronously emptyList()

        val sections = mutableListOf<Section>()
        val tasks = mutableListOf<Callable<Section>>()
        for (topic in topics) {
          tasks.add(Callable {
                      val section = Section()
                      section.name = topic.title
                      section.id = topic.id
                      if (topic.hasLessons) {
                        val lessonsIds = service.lessons(topic.id).execute().body()?.
                          lessons?.asSequence()?.filter { it.type != "test" }?.map { it.stepikId }?.toList() ?: return@Callable section
                        val lessons = getLessons(lessonsIds.map { it -> it.toString() }, languageId)
                        section.addLessons(lessons)
                        return@Callable section
                      }
                      return@Callable section
                     })
        }
        for (future in ConcurrencyUtil.invokeAll<Section>(tasks, EXECUTOR_SERVICE)) {
          if (!future.isCancelled) {
            val section = future.get()
            sections.add(section)
          }
        }
        sections.sortBy { it.id }
        return@runProcessWithProgressSynchronously sections
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

  private interface HyperskillLoggedIn {
    fun userLoggedIn()
  }
}
