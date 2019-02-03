package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.mixins.TaskSerializer
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepOptions
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.checkForErrors
import com.jetbrains.edu.learning.stepik.executeHandlingExceptions
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
  @JvmStatic
  val objectMapper: ObjectMapper

  init {
    val module = SimpleModule()
    module.addDeserializer(StepOptions::class.java, JacksonStepOptionsDeserializer())
    module.addDeserializer(Reply::class.java, StepikReplyDeserializer())
    objectMapper = StepikConnector.createMapper(module)

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
    get() = service(HyperskillSettings.INSTANCE.account)

  private fun service(account: HyperskillAccount?) : HyperskillService {
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

  // Authorization requests:

  fun doAuthorize(vararg postLoginActions: Runnable) {
    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val response = authorizationService.getTokens(CLIENT_ID, REDIRECT_URI, code, "authorization_code").executeHandlingExceptions()
    checkForErrors(response)
    val tokenInfo = response?.body() ?: return false
    val account = HyperskillAccount()
    account.tokenInfo = tokenInfo
    val currentUser = getCurrentUser(account) ?: return false
    account.userInfo = currentUser
    HyperskillSettings.INSTANCE.account = account
    ApplicationManager.getApplication().messageBus.syncPublisher<HyperskillLoggedIn>(authorizationTopic).userLoggedIn()
    return true
  }

  private fun HyperskillAccount.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val response = authorizationService.refreshTokens("refresh_token", CLIENT_ID, refreshToken).executeHandlingExceptions()
    checkForErrors(response)
    val tokens = response?.body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  // Get requests:

  fun getCurrentUser(account: HyperskillAccount): HyperskillUserInfo? {
    val response = service(account).getUserInfo(0).executeHandlingExceptions()
    checkForErrors(response)
    return response?.body()?.users?.firstOrNull() ?: return null
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    val response = service.stages(projectId).executeHandlingExceptions()
    checkForErrors(response)
    return response?.body()?.stages
  }

  fun getStepSources(lessonId: Int): List<StepSource>? {
    val response = service.steps(lessonId).executeHandlingExceptions()
    checkForErrors(response)
    return response?.body()?.steps
  }

  fun fillTopics(course: HyperskillCourse, project: Project) {
    for ((taskIndex, stage) in course.stages.withIndex()) {
      val call = service.topics(stage.id)
      call.enqueue(object: Callback<TopicsList> {
        override fun onFailure(call: Call<TopicsList>, t: Throwable) {
          LOG.warn("Failed to get topics for stage ${stage.id}")
        }

        override fun onResponse(call: Call<TopicsList>, response: Response<TopicsList>) {
          val topics = response.body()?.topics?.filter { it.theoryId != null}
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

  // Post requests:

  fun postSolution(task: Task, project: Project) {
    val taskDir = task.getTaskDir(project) ?: return LOG.error("Failed to find stage directory ${task.name}")

    val attempt = postAttempt(task.stepId) ?: return LOG.error("Failed to post attempt for stage ${task.stepId}")

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
    val module = SimpleModule()
    module.addSerializer(Task::class.java, TaskSerializer())
    val objectMapper = StepikConnector.createMapper(module)
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))
    val response = service.submission(Submission(score, attempt.id, files, serializedTask)).executeHandlingExceptions()
    checkForErrors(response)
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to make submission for stage ${task.stepId}")
    }
  }

  private fun postAttempt(step: Int): Attempt? {
    val response = service.attempt(Attempt(step)).executeHandlingExceptions()
    checkForErrors(response)
    return response?.body()?.attempts?.firstOrNull() ?: return null
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
