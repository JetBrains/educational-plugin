package com.jetbrains.edu.learning.marketplace

import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.CoursePageExperiment
import com.jetbrains.edu.learning.statistics.CoursePageExperimentManager
import com.jetbrains.edu.learning.ui.getUICheckLabel
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertNotNull as kAssertNotNull

// TODO: unify with `MarketplaceSubmissionsTest`
class MarketplaceSubmissionPostingTest : EduTestCase() {

  private val requestBodies: MutableMap<String, MutableList<String>> = ConcurrentHashMap()

  private lateinit var helper: MockWebServerHelper

  override fun setUp() {
    super.setUp()
    helper = MockWebServerHelper(testRootDisposable)
    mockkObject(SubmissionsServiceHost.Companion)
    Disposer.register(testRootDisposable) {
      unmockkObject(SubmissionsServiceHost.Companion)
    }
    every { SubmissionsServiceHost.getSelectedUrl() } returns helper.baseUrl
    mockJBAccount(testRootDisposable)
  }

  @Test
  fun `test empty submission metadata`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    val submissionRequestPath = "/api/course/${course.id}/${course.marketplaceCourseVersion}/task/${task.id}/submission"

    helper.addResponseHandlerWithRequestBodyRecording { request, path ->
      when (path) {
        submissionRequestPath -> MockResponseFactory.fromString(SUBMISSION_RESPONSE)
        else -> MockResponseFactory.badRequest()
      }
    }

    // when
    task.checkTask()
    PlatformTestUtil.waitWhileBusy { requestBodies[submissionRequestPath] == null }

    // then
    val body = kAssertNotNull(requestBodies[submissionRequestPath]?.singleOrNull(), "No request body found for $submissionRequestPath")
    val actualSubmission = MarketplaceSubmissionsConnector.getInstance().objectMapper.readValue<MarketplaceSubmission>(body)
    assertEquals(emptyMap<String, String>(), actualSubmission.metadata)
  }

  @Test
  fun `test submission metadata with active course page experiment`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    val experiment = CoursePageExperiment("123", "456")
    CoursePageExperimentManager.getInstance(project).experiment = experiment

    val submissionRequestPath = "/api/course/${course.id}/${course.marketplaceCourseVersion}/task/${task.id}/submission"

    helper.addResponseHandlerWithRequestBodyRecording { request, path ->
      when (path) {
        submissionRequestPath -> MockResponseFactory.fromString(SUBMISSION_RESPONSE)
        else -> MockResponseFactory.badRequest()
      }
    }

    // when
    task.checkTask()
    PlatformTestUtil.waitWhileBusy { requestBodies[submissionRequestPath] == null }

    // then
    val body = kAssertNotNull(requestBodies[submissionRequestPath]?.singleOrNull(), "No request body found for $submissionRequestPath")
    val actualSubmission = MarketplaceSubmissionsConnector.getInstance().objectMapper.readValue<MarketplaceSubmission>(body)
    assertEquals(
      mapOf("experiment_id" to experiment.experimentId, "experiment_variant" to experiment.experimentVariant),
      actualSubmission.metadata
    )
  }

  // TODO: unify with similar method from `com.jetbrains.edu.socialMedia.x.XConnectorTest`
  private fun MockWebServerHelper.addResponseHandlerWithRequestBodyRecording(handler: ResponseHandler) {
    addResponseHandler(testRootDisposable) { request, path ->
      val requests = requestBodies.getOrPut(path) {
        Collections.synchronizedList(mutableListOf())
      }
      requests += request.body.readUtf8()

      handler(request, path)
    }
  }

  private fun createEduCourse(): EduCourse {
    return courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse, id = 1) {
      lesson("lesson1") {
        eduTask("task1", stepId = 3) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 2
    } as EduCourse
  }

  private fun Task.checkTask() {
    NavigationUtils.navigateToTask(project, this)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    testAction(CheckAction(this.getUICheckLabel()))
  }

  companion object {
    private const val SUBMISSION_RESPONSE = """{"id":5749695,"time":"2025-06-11T11:25:17.97224012"}"""
  }
}
