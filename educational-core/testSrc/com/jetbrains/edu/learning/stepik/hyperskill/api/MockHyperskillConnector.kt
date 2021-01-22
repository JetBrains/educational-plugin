package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.*
import com.jetbrains.edu.learning.stepik.api.OPTIONS
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MockHyperskillConnector : HyperskillConnector() {

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  override val baseUrl: String get() = helper.baseUrl

  private var webSocketListener: WebSocketListener? = null

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockHyperskillConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }

  fun withWebSocketListener(listener: WebSocketListener) {
    webSocketListener = listener
  }

  override fun createWebSocket(client: OkHttpClient, url: String, listener: WebSocketListener): WebSocket {
    val webSocketMockSever = helper.webSocketMockSever
    val webSocket = client.newWebSocket(Request.Builder().url(webSocketMockSever.url("/")).build(), listener)
    webSocketMockSever.enqueue(MockResponseFactory.fromString("Mock Server Started").withWebSocketUpgrade(webSocketListener))
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
    withResponseHandler(disposable) { request ->
      MockResponseFactory.fromString(
        when (request.path) {
          "/api/projects/$projectId" -> objectMapper.writeValueAsString(ProjectsList().also { it.projects = listOf(hyperskillProject) })
          "/api/stages?project=$projectId" -> objectMapper.writeValueAsString(StagesList().also { it.stages = course.stages })
          "/api/steps?ids=${course.stages.map { it.stepId }.joinToString(separator = ",")}" -> stepSources(course.allTasks)
          else -> return@withResponseHandler null
        }
      )
    }
  }

  private fun configureProblemsResponses(disposable: Disposable, tasks: List<Task>) {
    withResponseHandler(disposable) { request ->
      val result = """/api/steps\?ids=(\d+)""".toRegex().matchEntire(request.path) ?: return@withResponseHandler null
      val stepId = result.groupValues[1].toInt()
      val task = tasks.find { it.id == stepId } ?: return@withResponseHandler null
      MockResponseFactory.fromString(stepSources(listOf(task)))
    }
  }

  private fun stepSources(tasks: List<Task>): String {
    val stepsList = HyperskillStepsList().apply {
      steps = tasks.map { task ->
        createStepSource(task)
      }
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
      lessonType = if (task.lesson is FrameworkLesson) EduNames.FRAMEWORK else null
    }

    return HyperskillStepSource().apply {
      id = task.id
      block = step
      title = task.name
    }
  }
}
