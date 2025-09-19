package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FRAMEWORK
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.BLOCK
import com.jetbrains.edu.learning.stepik.PYCHARM
import com.jetbrains.edu.learning.stepik.SOURCE
import com.jetbrains.edu.learning.stepik.Step
import com.jetbrains.edu.learning.stepik.api.MockStepikBasedConnector
import com.jetbrains.edu.learning.stepik.api.OPTIONS
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse

class MockHyperskillConnector : HyperskillConnector(), MockStepikBasedConnector {

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  override val baseUrl: String get() = helper.baseUrl

  private var webSocketListener: WebSocketListener? = null

  override fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockHyperskillConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }

  fun withWebSocketListener(listener: WebSocketListener) {
    webSocketListener = listener
  }

  override fun createWebSocket(client: OkHttpClient, url: String, listener: WebSocketListener): WebSocket {
    val webSocketMockSever = helper.webSocketMockSever
    val webSocket = client.newWebSocket(Request.Builder().url(webSocketMockSever.url("/")).build(), listener)
    var response = MockResponseFactory.fromString("Mock Server Started")
    webSocketListener?.let {
      response = response.withWebSocketUpgrade(it)
    }
    webSocketMockSever.enqueue(response)
    return webSocket
  }

  fun configureFromCourse(disposable: Disposable, course: HyperskillCourse) {
    if (course.hyperskillProject != null) {
      configureProjectResponses(disposable, course)
    }

    course.getProblemsLesson()?.let { lesson ->
      configureProblemsResponses(disposable, lesson.taskList)
    }
    course.getTopicsSection()?.let { section ->
      configureProblemsResponses(disposable, section.lessons.flatMap { it.taskList })
    }
  }

  private fun configureProjectResponses(disposable: Disposable, course: HyperskillCourse) {
    val hyperskillProject = course.hyperskillProject!!
    val projectId = hyperskillProject.id
    withResponseHandler(disposable) { request, _ ->
      val path = request.pathWithoutPrams
      MockResponseFactory.fromString(
        when (path) {
          "/api/projects/$projectId" -> objectMapper.writeValueAsString(ProjectsList().also { it.projects = listOf(hyperskillProject) })
          "/api/stages" if request.hasParams("project" to projectId.toString()) -> getStagesList(course.stages)
          "/api/steps" if request.hasParams("ids" to course.stages.map { it.stepId }.joinToString(separator = ",")) -> stepSources(course.allTasks)
          else -> return@withResponseHandler null
        }
      )
    }
  }

  private fun configureProblemsResponses(disposable: Disposable, tasks: List<Task>) {
    withResponseHandler(disposable) { _, path ->
      val result = """/api/steps\?ids=(\d+)&ide_rpc_port=(\d+)""".toRegex().matchEntire(path) ?: return@withResponseHandler null
      val stepId = result.groupValues[1].toInt()
      val task = tasks.find { it.id == stepId } ?: return@withResponseHandler null
      MockResponseFactory.fromString(stepSources(listOf(task)))
    }
  }

  fun mockResponseFromTask(task: Task, initStepSource: (HyperskillStepSource) -> Unit): MockResponse =
    MockResponseFactory.fromString(stepSources(listOf(task), initStepSource))

  private fun stepSources(tasks: List<Task>, initStepSource: (HyperskillStepSource) -> Unit = {}): String {
    val stepsList = HyperskillStepsList().apply {
      steps = tasks.map { task ->
        createStepSource(task).also(initStepSource)
      }
      meta = PaginationMetaData()
    }

    // HACK: rename "source" to "options"
    // This property has different names in educator and learner formats. Our (de)serializers are configured to support that.
    // However, here we need learners name in serialized step that's why we need to rename.
    val tree = objectMapper.valueToTree<JsonNode>(stepsList)
    for (node in tree.findValues(BLOCK)) {
      val objectNode = node as ObjectNode
      val source = objectNode.get(SOURCE)
      objectNode.set<JsonNode>(OPTIONS, source)
    }
    return objectMapper.writeValueAsString(tree)
  }

  private fun getStagesList(stages: List<HyperskillStage>): String =
    objectMapper.writeValueAsString(StagesList().also {
      it.meta = PaginationMetaData()
      it.stages = stages
    })

  private fun createStepSource(task: Task): HyperskillStepSource {
    val step = Step().apply {
      name = PYCHARM
      text = task.descriptionText
    }

    step.options = HyperskillStepOptions().apply {
      title = task.name
      descriptionFormat = task.descriptionFormat
      descriptionText = task.descriptionText
      files = task.taskFiles.values.toMutableList()
      taskType = task.itemType
      lessonType = if (task.lesson is FrameworkLesson) FRAMEWORK else null
    }

    return HyperskillStepSource().apply {
      id = task.id
      block = step
      title = task.name
    }
  }
}
