package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.AUTHORIZATION_CODE
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.REFRESH_TOKEN
import com.jetbrains.edu.learning.authUtils.OAuthUtils.checkBuiltinPortValid
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.checker.WebSocketConnectionState
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillTaskBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import okhttp3.*
import retrofit2.Call
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class HyperskillConnector {

  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper

  init {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    objectMapper = StepikConnector.createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  protected abstract val baseUrl: String

  private val authorizationService: HyperskillService
    get() {
      val retrofit = createRetrofitBuilder(baseUrl, connectionPool)
        .addConverterFactory(converterFactory)
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  private val service: HyperskillService
    get() = service(HyperskillSettings.INSTANCE.account)

  private fun service(account: HyperskillAccount?): HyperskillService {
    if (!isUnitTestMode && account != null && !account.tokenInfo.isUpToDate()) {
      account.refreshTokens()
    }

    val retrofit = createRetrofitBuilder(baseUrl, connectionPool, accessToken = account?.tokenInfo?.accessToken)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(HyperskillService::class.java)
  }

  // Authorization requests:

  fun doAuthorize(vararg postLoginActions: Runnable) {
    if (!checkBuiltinPortValid()) return

    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val response = authorizationService.getTokens(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, code,
                                                  AUTHORIZATION_CODE).executeHandlingExceptions()
    val tokenInfo = response?.body() ?: return false
    val account = HyperskillAccount()
    account.tokenInfo = tokenInfo
    val currentUser = getCurrentUser(account) ?: return false
    account.userInfo = currentUser
    HyperskillSettings.INSTANCE.account = account
    ApplicationManager.getApplication().messageBus.syncPublisher(AUTHORIZATION_TOPIC).userLoggedIn()
    return true
  }

  private fun HyperskillAccount.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val response = authorizationService.refreshTokens(REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET, refreshToken).executeHandlingExceptions()
    val tokens = response?.body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  // Get requests:

  fun getCurrentUser(account: HyperskillAccount): HyperskillProfileInfo? {
    val response = service(account).getCurrentUserInfo().executeHandlingExceptions()
    val userInfo = response?.body()?.profiles?.firstOrNull()
    if (userInfo?.isGuest == true) {
      // it means that session is broken and we should force user to relogin
      LOG.warn("User ${userInfo.fullname} ${userInfo.email} is anonymous")
      HyperskillSettings.INSTANCE.account = null
      return null
    }
    return userInfo
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    val response = service.stages(projectId).executeHandlingExceptions()
    return response?.body()?.stages
  }

  fun getProject(projectId: Int): Result<HyperskillProject, String> {
    return service.project(projectId).executeParsingErrors(true).flatMap {
      val result = it.body()?.projects?.firstOrNull()
      if (result == null) Err(it.message()) else Ok(result)
    }
  }

  private fun getStepSources(stepIds: List<Int>): Result<List<HyperskillStepSource>, String> =
    service.steps(stepIds.joinToString(separator = ",")).executeAndExtractFromBody().flatMap { Ok(it.steps) }

  fun getRecommendedStepsForTopic(topic: Int): Result<List<HyperskillStepSource>, String> =
    service.steps(topic).executeAndExtractFromBody().flatMap { Ok(it.steps) }

  fun getStepSource(stepId: Int): Result<HyperskillStepSource, String> =
    service.steps(stepId.toString()).executeAndExtractFromBody().flatMap {
      val result = it.steps.firstOrNull()
      if (result == null) Err("Can't get step source with $stepId id") else Ok(result)
    }

  fun fillTopics(course: HyperskillCourse, project: Project) {
    for ((taskIndex, stage) in course.stages.withIndex()) {
      val topics = getAllTopics(stage)
      if (topics.isEmpty()) continue

      course.taskToTopics[taskIndex] = topics
      runInEdt {
        if (project.isDisposed) return@runInEdt
        TaskDescriptionView.getInstance(project).updateAdditionalTab()
      }
    }
  }

  private fun getAllTopics(stage: HyperskillStage): List<HyperskillTopic> {
    var page = 1
    val topics = mutableListOf<HyperskillTopic>()
    do {
      val topicsList = service.topics(stage.id, page).executeHandlingExceptions(true)?.body() ?: break
      topics.addAll(topicsList.topics.filter { it.theoryId != null })
      page += 1
    }
    while (topicsList.topics.isNotEmpty() && topicsList.meta["has_next"] == true)
    return topics
  }

  fun getLesson(course: HyperskillCourse, attachmentLink: String): Lesson {
    val progressIndicator = ProgressManager.getInstance().progressIndicator

    val lesson = FrameworkLesson()
    lesson.index = 1
    lesson.course = course
    progressIndicator?.checkCanceled()
    val stepSources = getStepSources(course.stages.map { it.stepId }).onError { emptyList() }

    progressIndicator?.checkCanceled()
    val tasks = getTasks(course, lesson, stepSources)
    for (task in tasks) {
      lesson.addTask(task)
    }
    lesson.sortItems()
    loadAndFillAdditionalCourseInfo(course, attachmentLink)
    loadAndFillLessonAdditionalInfo(lesson)
    return lesson
  }

  fun getTasks(course: Course, lesson: Lesson, stepSources: List<HyperskillStepSource>): List<Task> {
    val tasks = ArrayList<Task>()
    for (step in stepSources) {
      val builder = HyperskillTaskBuilder(course, lesson, step, step.id)
      if (!builder.isSupported(step.block!!.name)) continue
      val task = builder.createTask(step.block!!.name)
      if (task != null) {
        tasks.add(task)
      }
    }
    return tasks
  }

  fun getProblems(course: Course, lesson: Lesson, steps: List<Int>): List<Task> {
    val stepSources = getStepSources(steps).onError { emptyList() }
    return getTasks(course, lesson, stepSources)
  }

  fun loadStages(hyperskillCourse: HyperskillCourse): Boolean {
    val hyperskillProject = hyperskillCourse.hyperskillProject ?: error("No Hyperskill project")
    val projectId = hyperskillProject.id
    if (hyperskillCourse.stages.isEmpty()) {
      val stages = getStages(projectId) ?: return false
      hyperskillCourse.stages = stages
    }
    val stages = hyperskillCourse.stages
    val lesson = getLesson(hyperskillCourse, hyperskillProject.ideFiles)
    if (lesson.taskList.size != stages.size) {
      LOG.warn("Course has ${stages.size} stages, but ${lesson.taskList.size} tasks")
      return false
    }

    lesson.taskList.forEachIndexed { index, task ->
      task.feedbackLink = feedbackLink(projectId, stages[index])
      task.name = stages[index].title
    }
    lesson.name = hyperskillCourse.name

    // We want project lesson to be the first
    // It's possible to open Problems in IDE without loading project lesson (stages)
    // So we need to update indices of existing Problems in this case
    if (hyperskillCourse.lessons.isNotEmpty()) {
      for (existingLesson in hyperskillCourse.lessons) {
        existingLesson.index += 1
      }
    }
    hyperskillCourse.addLesson(lesson)
    hyperskillCourse.sortItems()
    return true
  }

  private fun feedbackLink(project: Int, stage: HyperskillStage): FeedbackLink {
    return FeedbackLink("${stageLink(project, stage.id)}$HYPERSKILL_COMMENT_ANCHOR")
  }

  fun getSubmissions(stepIds: Set<Int>): List<Submission> {
    val userId = HyperskillSettings.INSTANCE.account?.userInfo?.id ?: return emptyList()
    var currentPage = 1
    val allSubmissions = mutableListOf<Submission>()
    while (true) {
      val submissionsList = service.submission(userId, stepIds.joinToString(separator = ","),
                                               currentPage).executeHandlingExceptions()?.body() ?: break
      val submissions = submissionsList.submissions
      allSubmissions.addAll(submissions)
      if (submissions.isEmpty() || !submissionsList.meta.containsKey("has_next") || submissionsList.meta["has_next"] == false) {
        break
      }
      currentPage += 1
    }
    return allSubmissions
  }

  fun getSubmissionById(submissionId: Int): Result<Submission, String> {
    return withTokenRefreshIfNeeded { service.submission(submissionId).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  fun getUser(userId: Int): Result<User, String> {
    return service.user(userId).executeAndExtractFirst(UsersList::users)
  }

  // Post requests:

  fun postSubmission(submission: Submission): Result<Submission, String> {
    return withTokenRefreshIfNeeded { service.submission(submission).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  fun postAttempt(step: Int): Result<Attempt, String> {
    return withTokenRefreshIfNeeded { service.attempt(Attempt(step)).executeAndExtractFirst(AttemptsList::attempts) }
  }

  fun markTheoryCompleted(step: Int): Result<Any, String> =
    withTokenRefreshIfNeeded { service.completeStep(step).executeParsingErrors(true) }

  fun getWebSocketConfiguration(): Result<WebSocketConfiguration, String> {
    return withTokenRefreshIfNeeded { service.websocket().executeAndExtractFromBody() }
  }

  fun sendFrontendEvents(events: List<HyperskillFrontendEvent>): Result<List<HyperskillFrontendEvent>, String> {
    return withTokenRefreshIfNeeded { service.sendFrontendEvents(events).executeAndExtractFromBody() }.map { it.events }
  }

  fun sendTimeSpentEvents(events: List<HyperskillTimeSpentEvent>): Result<List<HyperskillTimeSpentEvent>, String> {
    return withTokenRefreshIfNeeded { service.sendTimeSpentEvents(events).executeAndExtractFromBody() }.map { it.events }
  }

  private fun <T> Call<T>.executeAndExtractFromBody(): Result<T, String> {
    return executeParsingErrors(true).flatMap {
      val result = it.body()
      if (result == null) Err(it.message()) else Ok(result)
    }
  }

  private fun <T> withTokenRefreshIfNeeded(call: () -> Result<T, String>): Result<T, String> {
    val result = call()
    if (!isUnitTestMode && !ApplicationManager.getApplication().isInternal
        && result is Err && result.error == EduCoreBundle.message("error.access.denied")) {
      HyperskillSettings.INSTANCE.account?.refreshTokens()
      return call()
    }
    return result
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

  fun connectToWebSocketWithTimeout(timeOutSec: Long, url: String, initialState: WebSocketConnectionState): WebSocketConnectionState {

    fun logEvent(eventName: String, state: WebSocketConnectionState, message: String) =
      LOG.debug("WS: new event. Event=$eventName, state=${state::class.java.simpleName}, message=${message}")

    val client = OkHttpClient()
    val latch = CountDownLatch(1)
    var state = initialState
    val socket = createWebSocket(client, url, object : WebSocketListener() {
      private fun handleEvent(eventName: String, webSocket: WebSocket, message: String) {
        logEvent(eventName, state, message)
        try {
          state = state.handleEvent(webSocket, message)
          if (state.isTerminal) {
            latch.countDown()
          }
        }
        catch (e: Exception) {
          LOG.error(e)
          latch.countDown()
        }
      }

      override fun onOpen(webSocket: WebSocket, response: Response) {
        handleEvent("open", webSocket, response.message())
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logEvent("failure", state, response?.message() ?: "no message")
        latch.countDown()
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        handleEvent("message", webSocket, text)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logEvent("closure", state, reason)
        latch.countDown()
      }
    })

    latch.await(timeOutSec, TimeUnit.SECONDS)
    socket.close(1000, null)
    client.dispatcher().executorService().shutdown()
    return state
  }

  protected open fun createWebSocket(client: OkHttpClient, url: String, listener: WebSocketListener): WebSocket =
    client.newWebSocket(Request.Builder().url(url).build(), listener)

  companion object {
    private val LOG = Logger.getInstance("com.jetbrains.edu.learning.HyperskillConnector")

    @JvmStatic
    val AUTHORIZATION_TOPIC = com.intellij.util.messages.Topic.create("Edu.hyperskillLoggedIn", EduLogInListener::class.java)

    @JvmStatic
    fun getInstance(): HyperskillConnector = service()
  }

}
