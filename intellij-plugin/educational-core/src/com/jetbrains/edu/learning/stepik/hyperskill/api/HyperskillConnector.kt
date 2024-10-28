package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODE_ARGUMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTopic
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.network.executeParsingErrors
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.api.StepikBasedConnector.Companion.createObjectMapper
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.checker.WebSocketConnectionState
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillTaskBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.TOPICS_TAB
import okhttp3.*
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.ide.BuiltInServerManager
import retrofit2.Call
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class HyperskillConnector : EduOAuthCodeFlowConnector<HyperskillAccount, HyperskillUserInfo>(), StepikBasedConnector {
  override val platformName: String = HYPERSKILL

  override val redirectHost: String = "127.0.0.1"

  override var account: HyperskillAccount?
    get() = HyperskillSettings.INSTANCE.account
    set(account) {
      HyperskillSettings.INSTANCE.account = account
    }

  override val authorizationUrlBuilder: URIBuilder
    get() {
      return URIBuilder(HYPERSKILL_URL)
        .setPath("/oauth2/authorize/")
        .addParameter("client_id", CLIENT_ID)
        .addParameter("grant_type", CODE_ARGUMENT)
        .addParameter("redirect_uri", getRedirectUri())
        .addParameter("response_type", CODE_ARGUMENT)
        .addParameter("scope", "read write")
        .addParameter("utm_source", "ide")
        .addParameter("utm_medium", "ide")
        .addParameter("utm_campaign", "ide")
        .addParameter("utm_content", "login")
    }

  override val clientId: String = CLIENT_ID

  override val objectMapper: ObjectMapper by lazy {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    module.addDeserializer(Reply::class.java, HyperskillReplyDeserializer())
    createObjectMapper(module)
  }

  override val requestInterceptor: Interceptor = Interceptor { chain ->
    val request = chain.request()
    val newUrl = request.url.newBuilder().addQueryParameter("ide_rpc_port", BuiltInServerManager.getInstance().port.toString()).build()
    val newRequest = request.newBuilder().url(newUrl).build()
    chain.proceed(newRequest)
  }

  private val hyperskillEndpoints: HyperskillEndpoints
    get() = getEndpoints()

  override fun doRefreshTokens() {
    refreshTokens()
  }

  // Authorization requests:

  @Synchronized
  override fun login(code: String): Boolean {
    val tokenInfo = retrieveLoginToken(code, getRedirectUri()) ?: return false
    val account = HyperskillAccount(tokenInfo.expiresIn)
    val currentUser = getUserInfo(account, tokenInfo.accessToken) ?: return false
    if (currentUser.isGuest) {
      // it means that session is broken, so we should force user to re-login
      LOG.warn("User ${currentUser.getFullName()} ${currentUser.email} is anonymous")
      this.account = null
      return false
    }
    account.userInfo = currentUser
    account.saveTokens(tokenInfo)
    this.account = account
    return true
  }

  // Get requests:

  override fun getUserInfo(account: HyperskillAccount, accessToken: String?): HyperskillUserInfo? {
    val response = getEndpoints<HyperskillEndpoints>(account, accessToken).getCurrentUserInfo().executeHandlingExceptions()
    return response?.body()?.profiles?.firstOrNull()
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    return withPageIteration { page -> hyperskillEndpoints.stages(projectId, page).executeAndExtractFromBody() }
      .onError { return null }
      .flatMap { it.stages }
  }

  fun getProject(projectId: Int): Result<HyperskillProject, String> {
    return hyperskillEndpoints.project(projectId).executeParsingErrors(true).flatMap {
      val result = it.body()?.projects?.firstOrNull()
      if (result == null) Err(it.message()) else Ok(result)
    }
  }

  private fun getStepSources(stepIds: List<Int>): Result<List<HyperskillStepSource>, String> =
    withPageIteration { page ->
      hyperskillEndpoints.steps(
        stepIds.joinToString(separator = ","),
        page
      ).executeAndExtractFromBody()
    }.flatMap { hyperskillStepsLists -> Ok(hyperskillStepsLists.flatMap { it.steps }) }

  fun getStepsForTopic(topic: Int): Result<List<HyperskillStepSource>, String> =
    withPageIteration { page ->
      hyperskillEndpoints.steps(topic, page).executeAndExtractFromBody()
    }.flatMap { hyperskillStepsLists -> Ok(hyperskillStepsLists.flatMap { it.steps }) }

  fun getStepSource(stepId: Int): Result<HyperskillStepSource, String> =
    hyperskillEndpoints.steps(stepId.toString()).executeAndExtractFromBody().flatMap {
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
        TaskToolWindowView.getInstance(project).updateTab(TOPICS_TAB)
      }
    }
  }

  private fun getAllTopics(stage: HyperskillStage): List<HyperskillTopic> {
    return withPageIteration { hyperskillEndpoints.topics(stage.id, it).executeAndExtractFromBody() }
      .onError { return emptyList() }
      .flatMap { it.topics }
      .filter { it.theoryId != null }
  }

  fun getAdditionalFilesLink(hyperskillProjectId: Int): String {
    return "${baseUrl.withTrailingSlash()}api/projects/$hyperskillProjectId/additional-files/${StepikNames.ADDITIONAL_INFO}"
  }

  fun getLesson(course: HyperskillCourse): Lesson {
    val progressIndicator = ProgressManager.getInstance().progressIndicator

    val lesson = FrameworkLesson()
    lesson.index = 1
    lesson.parent = course
    progressIndicator?.checkCanceled()
    val stageIds = course.stages.map { it.stepId }
    val stepSources = getStepSources(stageIds).onError { e ->
      LOG.warn("Failed to load content for $stageIds stages because of: $e")
      emptyList()
    }

    progressIndicator?.checkCanceled()
    val tasks = getTasks(course, stepSources)
    for (task in tasks) {
      lesson.addTask(task)
    }
    lesson.sortItems()
    val hyperskillProject = course.hyperskillProject ?: error("No Hyperskill project")
    val attachmentLink = getAdditionalFilesLink(hyperskillProject.id)
    loadAndFillAdditionalCourseInfo(course, attachmentLink)
    loadAndFillLessonAdditionalInfo(lesson)
    return lesson
  }

  fun getProblems(course: Course, lesson: Lesson): List<Task> {
    val steps = lesson.taskList.map { it.id }
    val stepSources = getStepSources(steps).onError { emptyList() }
    return getTasks(course, stepSources)
  }

  fun loadStages(hyperskillCourse: HyperskillCourse) {
    val hyperskillProject = hyperskillCourse.hyperskillProject ?: error("No Hyperskill project")
    val projectId = hyperskillProject.id
    if (hyperskillCourse.stages.isEmpty()) {
      val stages = getStages(projectId) ?: return
      hyperskillCourse.stages = stages
    }
    val stages = hyperskillCourse.stages
    val lesson = getLesson(hyperskillCourse)
    if (lesson.taskList.size != stages.size) {
      LOG.warn("Course has ${stages.size} stages, but ${lesson.taskList.size} tasks")
      return
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
    return
  }

  private fun feedbackLink(project: Int, stage: HyperskillStage): String {
    return "${stageLink(project, stage.id)}$HYPERSKILL_COMMENT_ANCHOR"
  }

  fun getSubmissions(stepIds: Set<Int>): List<StepikBasedSubmission> {
    val userId = account?.userInfo?.id ?: return emptyList()
    return withPageIteration { page ->
      hyperskillEndpoints.submissions(userId, stepIds.joinToString(separator = ","), page).executeAndExtractFromBody()
    }
      .onError { return emptyList() }
      .flatMap { it.submissions }
  }

  override fun getSubmissions(stepId: Int) = getSubmissions(setOf(stepId))

  override fun getSubmission(id: Int): Result<StepikBasedSubmission, String> {
    return withTokenRefreshIfFailed { hyperskillEndpoints.submission(id).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  fun getUser(userId: Int): Result<User, String> {
    return hyperskillEndpoints.user(userId).executeAndExtractFirst(UsersList::users)
  }

  override fun getActiveAttempt(task: Task): Result<Attempt?, String> {
    return withTokenRefreshIfFailed {
      val userId = account?.userInfo?.id
                   ?: return@withTokenRefreshIfFailed Err("Trying to get list of attempts for unauthorized user")
      val attempts = withPageIteration { page -> hyperskillEndpoints.attempts(task.id, userId, page).executeAndExtractFromBody() }
        .onError { return@withTokenRefreshIfFailed Err(it) }
        .flatMap { it.attempts }

      val activeAttempt = attempts.firstOrNull { it.isActive && it.isRunning }
      Ok(activeAttempt)
    }
  }

  override fun getDataset(attempt: Attempt): Result<String, String> {
    return hyperskillEndpoints.dataset(attempt.id).executeParsingErrors().flatMap {
      val responseBody = it.body() ?: return@flatMap Err(EduCoreBundle.message("error.failed.to.parse.response"))
      Ok(responseBody.string())
    }
  }

  // Post requests:

  override fun postSubmission(submission: StepikBasedSubmission): Result<StepikBasedSubmission, String> {
    return withTokenRefreshIfFailed { hyperskillEndpoints.submission(submission).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  override fun postAttempt(task: Task): Result<Attempt, String> {
    return withTokenRefreshIfFailed { hyperskillEndpoints.attempt(Attempt(task.id)).executeAndExtractFirst(AttemptsList::attempts) }
  }

  fun markTheoryCompleted(step: Int): Result<Any, String> =
    withTokenRefreshIfFailed { hyperskillEndpoints.completeStep(step).executeParsingErrors(true) }

  fun getWebSocketConfiguration(): Result<WebSocketConfiguration, String> {
    return withTokenRefreshIfFailed { hyperskillEndpoints.websocket().executeAndExtractFromBody() }
  }

  fun sendFrontendEvents(events: List<HyperskillFrontendEvent>): Result<Any, String> {
    return withTokenRefreshIfFailed { hyperskillEndpoints.sendFrontendEvents(events).executeParsingErrors(true) }
  }

  fun sendTimeSpentEvents(events: List<HyperskillTimeSpentEvent>): Result<Any, String> {
    return withTokenRefreshIfFailed { hyperskillEndpoints.sendTimeSpentEvents(events).executeParsingErrors() }
  }

  private fun <T> Call<T>.executeAndExtractFromBody(): Result<T, String> {
    return executeParsingErrors(true).flatMap {
      val result = it.body()
      if (result == null) Err(it.message()) else Ok(result)
    }
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
        handleEvent("open", webSocket, response.message)
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logEvent("failure", state, response?.message ?: "no message")
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
    client.dispatcher.executorService.shutdown()
    return state
  }

  protected open fun createWebSocket(client: OkHttpClient, url: String, listener: WebSocketListener): WebSocket =
    client.newWebSocket(Request.Builder().url(url).build(), listener)

  private fun <T, R> Call<T>.executeAndExtractFirst(extractResult: T.() -> List<R>): Result<R, String> {
    return executeParsingErrors(true).flatMap {
      val result = it.body()?.extractResult()?.firstOrNull()
      if (result == null) Err(
        EduCoreBundle.message(
          "error.failed.to.post.solution.with.guide",
          EduNames.JBA,
          EduNames.FAILED_TO_POST_TO_JBA_URL
        )
      )
      else Ok(result)
    }
  }

  private fun String.withTrailingSlash(): String = if (!endsWith('/')) "$this/" else this

  companion object {
    private val LOG: Logger = logger<HyperskillConnector>()

    private val CLIENT_ID: String = HyperskillOAuthBundle.value("hyperskillClientId")

    fun getInstance(): HyperskillConnector = service()

    /**
     * Create new tasks in lesson. Tasks get from stepSources
     */
    fun getTasks(course: Course, stepSources: List<HyperskillStepSource>): List<Task> {
      val hyperskillCourse = course as HyperskillCourse
      return stepSources.mapNotNull { step ->
        HyperskillTaskBuilder(course, step).build()
          ?.also { hyperskillCourse.updateAdditionalFiles(step) }
      }
    }

    private fun HyperskillCourse.updateAdditionalFiles(stepSource: HyperskillStepSource) {
      val files = (stepSource.block?.options as? PyCharmStepOptions)?.hyperskill?.files ?: return
      additionalFiles = files.filter { taskFile ->
        taskFile.name !in additionalFiles.map { it.name }
      }
    }
  }
}
