package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.authUtils.OAuthRestService.CODE_ARGUMENT
import com.jetbrains.edu.learning.courseFormat.Course
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
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.TOPICS_TAB
import okhttp3.*
import org.apache.http.client.utils.URIBuilder
import retrofit2.Call
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class HyperskillConnector : EduOAuthConnector<HyperskillAccount, HyperskillUserInfo>(), StepikBasedConnector {
  override val displayName: String = EduNames.JBA
  override val platformName: String = HYPERSKILL

  override var account: HyperskillAccount?
    get() = HyperskillSettings.INSTANCE.account
    set(account) {
      HyperskillSettings.INSTANCE.account = account
    }

  override val authorizationUrl: String
    get() {
      val url = URIBuilder(HYPERSKILL_URL)
        .setPath("/oauth2/authorize/")
        .addParameter("client_id", CLIENT_ID)
        .addParameter("grant_type", CODE_ARGUMENT)
        .addParameter("redirect_uri", getRedirectUri())
        .addParameter("response_type", CODE_ARGUMENT)
        .addParameter("scope", "read write")
        .build()
        .toString()
      return wrapWithUtm(url, "login")
    }

  override val clientId: String = CLIENT_ID

  override val clientSecret: String = CLIENT_SECRET

  override val objectMapper: ObjectMapper by lazy {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    StepikConnector.createObjectMapper(module)
  }

  private val hyperskillEndpoints: HyperskillEndpoints
    get() = getEndpoints()

  // Authorization requests:

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
    val response = hyperskillEndpoints.stages(projectId).executeHandlingExceptions()
    return response?.body()?.stages
  }

  fun getProject(projectId: Int): Result<HyperskillProject, String> {
    return hyperskillEndpoints.project(projectId).executeParsingErrors(true).flatMap {
      val result = it.body()?.projects?.firstOrNull()
      if (result == null) Err(it.message()) else Ok(result)
    }
  }

  private fun getStepSources(stepIds: List<Int>): Result<List<HyperskillStepSource>, String> =
    hyperskillEndpoints.steps(stepIds.joinToString(separator = ",")).executeAndExtractFromBody().flatMap { Ok(it.steps) }

  fun getStepsForTopic(topic: Int): Result<List<HyperskillStepSource>, String> =
    hyperskillEndpoints.steps(topic).executeAndExtractFromBody().flatMap { Ok(it.steps) }

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
        TaskDescriptionView.getInstance(project).updateTab(TOPICS_TAB)
      }
    }
  }

  private fun getAllTopics(stage: HyperskillStage): List<HyperskillTopic> {
    var page = 1
    val topics = mutableListOf<HyperskillTopic>()
    do {
      val topicsList = hyperskillEndpoints.topics(stage.id, page).executeHandlingExceptions(true)?.body() ?: break
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

  private fun feedbackLink(project: Int, stage: HyperskillStage): String {
    return "${stageLink(project, stage.id)}$HYPERSKILL_COMMENT_ANCHOR"
  }

  fun getSubmissions(stepIds: Set<Int>): List<StepikBasedSubmission> {
    val userId = account?.userInfo?.id ?: return emptyList()
    var currentPage = 1
    val allSubmissions = mutableListOf<StepikBasedSubmission>()
    while (true) {
      val submissionsList = hyperskillEndpoints.submission(userId, stepIds.joinToString(separator = ","),
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

  override fun getSubmission(id: Int): Result<StepikBasedSubmission, String> {
    return withTokenRefreshIfNeeded { hyperskillEndpoints.submission(id).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  fun getUser(userId: Int): Result<User, String> {
    return hyperskillEndpoints.user(userId).executeAndExtractFirst(UsersList::users)
  }

  override fun getActiveAttempt(task: Task): Result<Attempt?, String> {
    return withTokenRefreshIfNeeded {
      val userId = account?.userInfo?.id
                   ?: return@withTokenRefreshIfNeeded Err("Attempt to get list of attempts for unauthorized user")
      val attempts = hyperskillEndpoints.attempts(task.id, userId).executeParsingErrors(true).flatMap {
        val result = it.body()?.attempts
        if (result == null) Err(it.message()) else Ok(result)
      }.onError { return@withTokenRefreshIfNeeded Err(it) }

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
    return withTokenRefreshIfNeeded { hyperskillEndpoints.submission(submission).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  override fun postAttempt(task: Task): Result<Attempt, String> {
    return withTokenRefreshIfNeeded { hyperskillEndpoints.attempt(Attempt(task.id)).executeAndExtractFirst(AttemptsList::attempts) }
  }

  fun markTheoryCompleted(step: Int): Result<Any, String> =
    withTokenRefreshIfNeeded { hyperskillEndpoints.completeStep(step).executeParsingErrors(true) }

  fun getWebSocketConfiguration(): Result<WebSocketConfiguration, String> {
    return withTokenRefreshIfNeeded { hyperskillEndpoints.websocket().executeAndExtractFromBody() }
  }

  fun sendFrontendEvents(events: List<HyperskillFrontendEvent>): Result<List<HyperskillFrontendEvent>, String> {
    return withTokenRefreshIfNeeded { hyperskillEndpoints.sendFrontendEvents(events).executeAndExtractFromBody() }.map { it.events }
  }

  fun sendTimeSpentEvents(events: List<HyperskillTimeSpentEvent>): Result<List<HyperskillTimeSpentEvent>, String> {
    return withTokenRefreshIfNeeded { hyperskillEndpoints.sendTimeSpentEvents(events).executeAndExtractFromBody() }.map { it.events }
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
      refreshTokens()
      return call()
    }
    return result
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
    private val LOG: Logger = logger<HyperskillConnector>()

    private val CLIENT_ID: String = HyperskillOAuthBundle.value("hyperskillClientId")
    private val CLIENT_SECRET: String = HyperskillOAuthBundle.value("hyperskillClientSecret")

    @JvmStatic
    fun getInstance(): HyperskillConnector = service()

    /**
     * Create new tasks in lesson. Tasks get from stepSources
     */
    fun getTasks(course: Course, lesson: Lesson, stepSources: List<HyperskillStepSource>): List<Task> {
      val hyperskillCourse = course as HyperskillCourse
      return stepSources.mapNotNull { step ->
        HyperskillTaskBuilder(course, lesson, step).build()
          ?.also { hyperskillCourse.updateAdditionalFiles(step) }
      }
    }

    fun HyperskillCourse.updateAdditionalFiles(stepSource: HyperskillStepSource) {
      val files = (stepSource.block?.options as? PyCharmStepOptions)?.hyperskill?.files ?: return
      additionalFiles.addAll(files.filter { taskFile ->
        taskFile.name !in additionalFiles.map { it.name }
      })
    }
  }
}
