package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepikUserAgent
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import okhttp3.ConnectionPool
import org.apache.http.HttpStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*

abstract class HyperskillConnector {

  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper

  init {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    module.addDeserializer(Reply::class.java, StepikReplyDeserializer())
    objectMapper = StepikConnector.createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  protected abstract val baseUrl: String

  private val authorizationService: HyperskillService
    get() {
      val retrofit = createRetrofitBuilder(baseUrl, connectionPool, stepikUserAgent)
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

    val retrofit = createRetrofitBuilder(baseUrl, connectionPool, accessToken = account?.tokenInfo?.accessToken)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(HyperskillService::class.java)
  }

  // Authorization requests:

  fun doAuthorize(vararg postLoginActions: Runnable) {
    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val response = authorizationService.getTokens(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, code, "authorization_code").executeHandlingExceptions()
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
    val response = authorizationService.refreshTokens("refresh_token", CLIENT_ID, CLIENT_SECRET, refreshToken).executeHandlingExceptions()
    val tokens = response?.body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  // Get requests:

  fun getCurrentUser(account: HyperskillAccount): HyperskillUserInfo? {
    val response = service(account).getCurrentUserInfo().executeHandlingExceptions()
    return response?.body()?.profiles?.firstOrNull()
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    val response = service.stages(projectId).executeHandlingExceptions()
    return response?.body()?.stages
  }

  fun getProject(projectId: Int): HyperskillProject? {
    val response = service.project(projectId).executeHandlingExceptions()
    return response?.body()?.projects?.firstOrNull()
  }

  fun getStepSource(stepId: Int): HyperskillStepSource? {
    val response = service.step(stepId).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
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
              if (project.isDisposed) return@runInEdt
              TaskDescriptionView.getInstance(project).updateAdditionalTaskTab()
            }
          }
        }
      })
    }
  }

  fun getLesson(course: HyperskillCourse, attachmentLink: String): Lesson? {
    val progressIndicator = ProgressManager.getInstance().progressIndicator

    val lesson = FrameworkLesson()
    lesson.course = course
    progressIndicator?.checkCanceled()
    progressIndicator?.text2 = "Loading project stages"
    val stepSources = course.stages.mapNotNull { getStepSource(it.stepId) }

    progressIndicator?.checkCanceled()
    val tasks = getTasks(course, lesson, stepSources)
    for (task in tasks) {
      lesson.addTask(task)
    }
    loadAndFillAttachmentsInfo(course, attachmentLink)
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

  fun fillHyperskillCourse(hyperskillCourse: HyperskillCourse): Boolean {
    return try {
      ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.WithResult<Boolean, Exception>(null, "Loading hyperskill project", true) {
        override fun compute(indicator: ProgressIndicator): Boolean {
          val hyperskillAccount = HyperskillSettings.INSTANCE.account
          if (hyperskillAccount == null) {
            LOG.error("User is not logged in to the Hyperskill")
            return false
          }
          val hyperskillProject = hyperskillCourse.hyperskillProject ?: error("Disconnected Hyperskill project")
          if (!hyperskillProject.useIde) {
            LOG.error("Selected project is not supported")
            return false
          }
          val projectId = hyperskillProject.id

          if (hyperskillCourse.stages.isEmpty()) {
            val stages = getStages(projectId) ?: return false
            hyperskillCourse.stages = stages
          }
          val stages = hyperskillCourse.stages
          val lesson = getLesson(hyperskillCourse, hyperskillProject.ideFiles)
          if (lesson == null) {
            LOG.warn("Project doesn't contain framework lesson")
            return false
          }
          if (lesson.taskList.size != stages.size) {
            LOG.warn("Course has ${stages.size} stages, but ${lesson.taskList.size} tasks")
            return false
          }

          lesson.taskList.forEachIndexed { index, task ->
            task.feedbackLink = feedbackLink(projectId, stages[index])
            task.name = stages[index].title
          }
          lesson.name = hyperskillProject.title

          hyperskillCourse.addLesson(lesson)
          return true
        }
      })
    }
    catch (e: Exception) {
      LOG.warn(e)
      false
    }
  }

  fun feedbackLink(project: Int, stage: HyperskillStage): FeedbackLink {
    return FeedbackLink("$HYPERSKILL_PROJECTS_URL/$project/stages/${stage.id}/implement")
  }

  fun getSubmission(stepId: Int, page: Int = 1): Submission? {
    return service.submission(stepId, page).executeHandlingExceptions()?.body()?.submissions?.firstOrNull()
  }

  fun getSubmissionById(submissionId: Int): Submission? {
    return service.submission(submissionId).executeHandlingExceptions()?.body()?.submissions?.firstOrNull()
  }

  fun getSolution(stepId: Int): Solution? {
    return service.solutions(stepId).executeHandlingExceptions()?.body()?.solutions?.firstOrNull()
  }

  // Post requests:

  fun postSubmission(submission: Submission): Submission? {
    val response = service.submission(submission).executeHandlingExceptions()
    if (response == null || response.code() != HttpStatus.SC_CREATED) {
      showFailedToPostNotification()
      LOG.error("Failed to make submission")
    }
    return response?.body()?.submissions?.firstOrNull()
  }

  fun postAttempt(step: Int): Attempt? {
    val response = service.attempt(Attempt(step)).executeHandlingExceptions()
    return response?.body()?.attempts?.firstOrNull()
  }

  private fun createAuthorizationListener(vararg postLoginActions: Runnable) {
    authorizationBusConnection.disconnect()
    authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
    authorizationBusConnection.subscribe(AUTHORIZATION_TOPIC, object : EduLogInListener {
      override fun userLoggedOut() { }

      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
      }
    })
  }

  companion object {
    private val LOG = Logger.getInstance(HyperskillConnector::class.java)

    @JvmStatic
    val AUTHORIZATION_TOPIC = com.intellij.util.messages.Topic.create("Edu.hyperskillLoggedIn", EduLogInListener::class.java)

    @JvmStatic
    fun getInstance(): HyperskillConnector = service()
  }

}
