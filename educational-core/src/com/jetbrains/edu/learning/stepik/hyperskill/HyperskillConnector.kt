package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikSteps
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.apache.http.HttpStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

object HyperskillConnector {
  private val LOG = Logger.getInstance(HyperskillConnector::class.java)

  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
  private val authorizationTopic = com.intellij.util.messages.Topic.create<HyperskillLoggedIn>("Edu.hyperskillLoggedIn",
                                                                                               HyperskillLoggedIn::class.java)
  private val converterFactory: JacksonConverterFactory

  init {
    val module = SimpleModule()
    module.addDeserializer(StepikSteps.FileWrapper::class.java, FileWrapperDeserializer())
    val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.registerModule(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private val authorizationService: HyperskillService
    get() {
      val retrofit = Retrofit.Builder()
        .baseUrl(HYPERSKILL_URL)
        .addConverterFactory(converterFactory)
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  private val service: HyperskillService
    get() {
      val account = HyperskillSettings.INSTANCE.account
      if (account != null && !account.tokenInfo.isUpToDate()) {
        account.refreshTokens()
      }

      val dispatcher = Dispatcher()
      dispatcher.maxRequests = 10

      val okHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
          val tokenInfo = account?.tokenInfo
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
        .addConverterFactory(converterFactory)
        .client(okHttpClient)
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  fun doAuthorize(vararg postLoginActions: Runnable) {
    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val tokenInfo = authorizationService.getTokens(CLIENT_ID, REDIRECT_URI, code, "authorization_code").execute().body() ?: return false
    HyperskillSettings.INSTANCE.account = HyperskillAccount()
    HyperskillSettings.INSTANCE.account!!.tokenInfo = tokenInfo
    val currentUser = getCurrentUser() ?: return false
    HyperskillSettings.INSTANCE.account!!.userInfo = currentUser
    ApplicationManager.getApplication().messageBus.syncPublisher<HyperskillLoggedIn>(authorizationTopic).userLoggedIn()
    return true
  }

  private fun HyperskillAccount.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val tokens = authorizationService.refreshTokens("refresh_token", CLIENT_ID, refreshToken).execute().body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  fun getCurrentUser(): HyperskillUserInfo? {
    return service.getUserInfo(0).execute().body()?.users?.firstOrNull() ?: return null
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    return service.stages(projectId).execute().body()?.stages
  }

  fun getStepSources(lessonId: Int): List<StepikSteps.StepSource>? {
    return service.steps(lessonId).execute().body()?.steps
  }

  fun postSolution(task: Task, project: Project) {
    val taskDir = task.getTaskDir(project) ?: return LOG.error("Failed to find task directory ${task.name}")

    val attempt = postAttempt(task.stepId) ?: return LOG.error("Failed post attempt for task ${task.stepId}")

    val files = ArrayList<SolutionFile>()
    for (taskFile in task.taskFiles.values) {
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      runReadAction {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
        files.add(SolutionFile(taskFile.name, document.text))
      }
    }

    postSubmission(attempt, files, task)
  }

  private fun postSubmission(attempt: Attempt, files: ArrayList<SolutionFile>, task: Task) {
    val score = if (task.status == CheckStatus.Solved) "1" else "0"
    val reply = Reply(score, files)
    val response = service.submission(Submission(attempt.id, reply)).execute()

    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to make submission for task ${task.stepId}")
    }
  }

  private fun postAttempt(step: Int): Attempt? {
    return service.attempt(step).execute().body()?.attempts?.firstOrNull() ?: return null
  }

  fun fillTopics(course: HyperskillCourse, project: Project) {
    for ((taskIndex, stage) in course.stages.withIndex()) {
      val call = service.topics(stage.id)
      call.enqueue(object: Callback<TopicsList> {
        override fun onFailure(call: Call<TopicsList>, t: Throwable) {
          LOG.warn("Failed to get topics for stage ${stage.id}")
        }

        override fun onResponse(call: Call<TopicsList>, response: Response<TopicsList>) {
          val topics = response.body()?.topics?.filter { it.children.isEmpty() }
          if (topics != null && topics.isNotEmpty()) {
            course.taskToTopics[taskIndex] = topics
            runInEdt {
              TaskDescriptionView.getInstance(project).updateAdditionalTaskTab()
            }
          }
        }
      })
    }
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

class FileWrapperDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<StepikSteps.FileWrapper>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): StepikSteps.FileWrapper {
    val node: JsonNode = jp.codec.readTree(jp)
    val name = node.get("name").asText()
    val text = StringUtil.convertLineSeparators(node.get("text").asText())
    return StepikSteps.FileWrapper(name, text)
  }
}
