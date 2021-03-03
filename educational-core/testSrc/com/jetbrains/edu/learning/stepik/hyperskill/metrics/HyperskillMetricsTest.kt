package com.jetbrains.edu.learning.stepik.hyperskill.metrics

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.stepik.hyperskill.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourseWithFiles
import java.nio.file.Paths
import java.util.*

class HyperskillMetricsTest : EduTestCase() {
  private val metricsService: HyperskillMetricsService get() = HyperskillMetricsService.getInstance()
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun tearDown() {
    try {
      metricsService.allEvents(emptyQueue = true)
    }
    finally {
      super.tearDown()
    }
  }

  fun `test current serialization format`() {
    val hyperskillCourse = createHyperskillCourse()

    val addedEvents = addViewEvents(hyperskillCourse, listOf(findTask(0, 0), findTask(1, 0)))

    serializeAndDeserializeBack()

    val deserializedEvents = metricsService.allEvents(emptyQueue = false)
    compareEvents(addedEvents, deserializedEvents)
  }

  /**
   * This test is needed to make sure that original xml format can be deserialized properly
   * If xml format changes, please add a new test, do not simply modify this one
   * [HyperskillMetricsTest.test serialization format] checks if current format can be deserialized
   */
  fun `test events deserialization from xml`() {
    val stateFromFile = deserializeFromFile("hyperskill_events.xml")

    val expectedEvents = listOf(createHyperskillEvent("/projects/41/stages/214/implement", Date(111)),
                                createHyperskillEvent("/learn/step/123", Date(222)))

    compareEvents(expectedEvents, stateFromFile.events)
  }

  fun `test serialization limit respected`() {
    val hyperskillCourse = createHyperskillCourse()

    val addedEvents = addViewEvents(hyperskillCourse, List(HyperskillMetricsService.EVENTS_LIMIT + 1) { findTask(0, 0) })
    assertEquals(HyperskillMetricsService.EVENTS_LIMIT + 1, addedEvents.size)

    serializeAndDeserializeBack()

    val deserializedEvents = metricsService.allEvents(emptyQueue = false)
    compareEvents(addedEvents.subList(0, HyperskillMetricsService.EVENTS_LIMIT), deserializedEvents)
  }

  fun `test all events sent`() {
    val hyperskillCourse = createHyperskillCourse()

    addViewEvents(hyperskillCourse, listOf(findTask(0, 0), findTask(1, 0)))

    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (request.path) {
          "/api/frontend-events" -> """{"${FRONTEND_EVENTS}":[]}"""
          else -> return@withResponseHandler null
        }
      )
    }

    HyperskillMetricsScheduler.sendEvents()

    val pendingEvents = metricsService.allEvents(false)
    assertEmpty(pendingEvents)
  }

  fun `test no events sent`() {
    val hyperskillCourse = createHyperskillCourse()

    val viewEvents = addViewEvents(hyperskillCourse, listOf(findTask(0, 0), findTask(1, 0)))
    HyperskillMetricsScheduler.sendEvents()

    val pendingEvents = metricsService.allEvents(false)
    compareEvents(viewEvents, pendingEvents)
  }

  fun `test events sent in chunks`() {
    val hyperskillCourse = createHyperskillCourse()

    val eventsCount = 5000

    val expectedChunks = eventsCount / HyperskillMetricsScheduler.EVENTS_PER_REQUEST +
                         (if (eventsCount % HyperskillMetricsScheduler.EVENTS_PER_REQUEST == 0) 0 else 1)

    var chunksCount = 0
    addViewEvents(hyperskillCourse, List(eventsCount) { findTask(0, 0) })

    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (request.path) {
          "/api/frontend-events" -> {
            chunksCount++
            """{"${FRONTEND_EVENTS}":[]}"""
          }
          else -> return@withResponseHandler null
        }
      )
    }

    HyperskillMetricsScheduler.sendEvents()
    assertEquals(expectedChunks, chunksCount)

    val pendingEvents = metricsService.allEvents(false)
    assertEmpty(pendingEvents)
  }

  fun `test pending events limit respected`() {
    val hyperskillCourse = createHyperskillCourse()

    val addedEvents = addViewEvents(hyperskillCourse, List(HyperskillMetricsService.EVENTS_LIMIT + 1) { findTask(0, 0) })
    HyperskillMetricsScheduler.sendEvents()

    val pendingEvents = metricsService.allEvents(false)
    compareEvents(addedEvents.subList(0, HyperskillMetricsService.EVENTS_LIMIT), pendingEvents)
  }

  private fun createHyperskillCourse() = hyperskillCourseWithFiles {
    frameworkLesson("lesson1") {
      eduTask("task1", stepId = 1) {
        taskFile("src/Task.kt", "stage 1")
        taskFile("test/Tests1.kt", "stage 1 test")
      }
      eduTask("task2", stepId = 2) {
        taskFile("src/Task.kt", "stage 2")
        taskFile("test/Tests2.kt", "stage 2 test")
      }
    }

    lesson(HYPERSKILL_PROBLEMS) {
      codeTask(stepId = 4) {
        taskFile("task.txt", "file text")
      }
    }
  }

  private fun compareEvents(expectedEvents: List<HyperskillFrontendEvent>, actualEvents: List<HyperskillFrontendEvent>) {
    assertEquals(expectedEvents.size, actualEvents.size)
    actualEvents.forEachIndexed { index, actualEvent ->
      val expectedEvent = expectedEvents[index]

      assertEquals(expectedEvent.route, actualEvent.route)
      assertEquals(expectedEvent.action, actualEvent.action)
      assertEquals(expectedEvent.clientTime, actualEvent.clientTime)
    }
  }

  private fun serializeAndDeserializeBack() {
    val serialized = XmlSerializer.serialize(metricsService.state)
    metricsService.allEvents(emptyQueue = true)
    metricsService.loadState(XmlSerializer.deserialize(serialized, HyperskillMetricsService.State::class.java))
  }

  private fun addViewEvents(hyperskillCourse: HyperskillCourse, tasks: List<Task>): List<HyperskillFrontendEvent> {
    tasks.forEach { metricsService.doAddViewEvent(hyperskillCourse, it) }
    return metricsService.allEvents(emptyQueue = false)
  }

  private fun createHyperskillEvent(eventRoute: String,
                                    eventTime: Date,
                                    eventAction: HyperskillFrontendEventType = HyperskillFrontendEventType.VIEW): HyperskillFrontendEvent {
    return HyperskillFrontendEvent().apply {
      route = eventRoute
      action = eventAction
      clientTime = eventTime
    }
  }

  private fun deserializeFromFile(name: String): HyperskillMetricsService.State {
    val filePath = Paths.get(testDataPath).resolve(name)
    return XmlSerializer.deserialize(JDOMUtil.load(filePath), HyperskillMetricsService.State::class.java)
  }

  override fun getTestDataPath(): String = "testData/stepik/hyperskill/metrics"
}