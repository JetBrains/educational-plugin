package com.jetbrains.edu.learning.marketplace

import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.application
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.UserAgreementProperties
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.UserAgreement
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.network.HTTP_UNAVAILABLE_FOR_LEGAL_REASONS
import com.jetbrains.edu.learning.statistics.metadata.CourseSubmissionMetadataManager
import com.jetbrains.edu.learning.submissions.SubmissionsListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.submissions.UserAgreementState.ACCEPTED
import com.jetbrains.edu.learning.submissions.UserAgreementState.DECLINED
import com.jetbrains.edu.learning.submissions.UserAgreementState.NOT_SHOWN
import com.jetbrains.edu.learning.ui.getUICheckLabel
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import java.net.HttpURLConnection.HTTP_UNAVAILABLE
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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
    every { SubmissionsServiceHost.selectedHost } returns SelectedServiceHost(SubmissionsServiceHost.OTHER, helper.baseUrl)
    mockJBAccount(testRootDisposable)
  }

  @Test
  fun `test empty submission metadata`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    val submissionRequestPath = "/api/course/${course.id}/${course.marketplaceCourseVersion}/task/${task.id}/submission"

    helper.addResponseHandlerWithRequestBodyRecording { _, path ->
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
  fun `test submission metadata`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    CourseSubmissionMetadataManager.getInstance(project).addMetadata("foo" to "bar", "123" to "456")

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
      mapOf("foo" to "bar", "123" to "456"),
      actualSubmission.metadata
    )
  }

  @Test
  fun `test submission retries`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    val submissionRequestPath = "/api/course/${course.id}/${course.marketplaceCourseVersion}/task/${task.id}/submission"

    val counter = AtomicInteger()
    helper.addResponseHandlerWithRequestBodyRecording { _, path ->
      when (path) {
        submissionRequestPath -> {
          val attempt = counter.incrementAndGet()
          if (attempt == 1) MockResponse().setResponseCode(HTTP_UNAVAILABLE) else MockResponseFactory.fromString(SUBMISSION_RESPONSE)
        }
        else -> MockResponseFactory.badRequest()
      }
    }

    val submissionsChanged = AtomicBoolean()
    project.messageBus.connect(testRootDisposable).subscribe(SubmissionsManager.TOPIC, SubmissionsListener {
      submissionsChanged.set(true)
    })

    // when
    task.checkTask()
    PlatformTestUtil.waitWhileBusy { !submissionsChanged.get() }

    // then
    assertEquals(2, counter.get())

    val submission = kAssertNotNull(SubmissionsManager.getInstance(project).getSubmissions(task)?.singleOrNull())
    assertEquals(SUBMISSION_ID, submission.id)
  }

  @Test
  fun `test submission successful from first attempt`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    val submissionRequestPath = "/api/course/${course.id}/${course.marketplaceCourseVersion}/task/${task.id}/submission"

    val counter = AtomicInteger()
    helper.addResponseHandlerWithRequestBodyRecording { _, path ->
      when (path) {
        submissionRequestPath -> {
          counter.incrementAndGet()
          MockResponseFactory.fromString(SUBMISSION_RESPONSE)
        }
        else -> MockResponseFactory.badRequest()
      }
    }

    val submissionsChanged = AtomicBoolean()
    project.messageBus.connect(testRootDisposable).subscribe(SubmissionsManager.TOPIC, SubmissionsListener {
      submissionsChanged.set(true)
    })

    // when
    task.checkTask()
    PlatformTestUtil.waitWhileBusy { !submissionsChanged.get() }

    // then
    assertEquals(1, counter.get())

    val submission = kAssertNotNull(SubmissionsManager.getInstance(project).getSubmissions(task)?.singleOrNull())
    assertEquals(SUBMISSION_ID, submission.id)
  }

  @Test
  fun `test remote user agreement is not syncs with locate state`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    val userAgreementSettings = mockService<UserAgreementSettings>(application)
    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties.fullyAccepted())

    val remoteAgreementState = AtomicReference(UserAgreement(NOT_SHOWN, NOT_SHOWN))

    val submissionRequestPath = "/api/course/${course.id}/${course.marketplaceCourseVersion}/task/${task.id}/submission"

    helper.addResponseHandler(testRootDisposable) { request, _ ->
      when (request.pathWithoutPrams) {
        "/api/v2/agreement/update" -> {
          val userAgreement = request.userAgreement() ?: return@addResponseHandler MockResponseFactory.badRequest()
          remoteAgreementState.set(userAgreement)
          MockResponseFactory.ok()
        }
        submissionRequestPath -> {
          if (remoteAgreementState.get().pluginAgreement != ACCEPTED) {
            MockResponse().setResponseCode(HTTP_UNAVAILABLE_FOR_LEGAL_REASONS)
          }
          else {
            MockResponseFactory.fromString(SUBMISSION_RESPONSE)
          }
        }
        else -> MockResponseFactory.badRequest()
      }
    }

    val submissionsChanged = AtomicBoolean()
    project.messageBus.connect(testRootDisposable).subscribe(SubmissionsManager.TOPIC, SubmissionsListener {
      submissionsChanged.set(true)
    })

    // when
    task.checkTask()
    PlatformTestUtil.waitWhileBusy { !submissionsChanged.get() }

    // then
    assertEquals(UserAgreement(ACCEPTED, ACCEPTED), remoteAgreementState.get())
    val submission = kAssertNotNull(SubmissionsManager.getInstance(project).getSubmissions(task)?.singleOrNull())
    assertEquals(SUBMISSION_ID, submission.id)
  }

  @Test
  fun `test remote user agreement is not syncs with locate state but cannot be update`() {
    // given
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")

    val userAgreementSettings = mockService<UserAgreementSettings>(application)
    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties.pluginAgreementAccepted())

    val submissionRequestPath = "/api/course/${course.id}/${course.marketplaceCourseVersion}/task/${task.id}/submission"

    val sentUserAgreement = AtomicReference<UserAgreement>(null)
    val submissionRequestReceived = AtomicBoolean()

    helper.addResponseHandler(testRootDisposable) { request, _ ->
      when (request.pathWithoutPrams) {
        "/api/v2/agreement/update" -> {
          val userAgreement = request.userAgreement()
          if (userAgreement != null) {
            sentUserAgreement.set(userAgreement)
          }
          MockResponseFactory.badRequest()
        }
        submissionRequestPath -> {
          submissionRequestReceived.set(true)
          MockResponse().setResponseCode(HTTP_UNAVAILABLE_FOR_LEGAL_REASONS)
        }
        else -> MockResponseFactory.badRequest()
      }
    }

    // when
    task.checkTask()
    PlatformTestUtil.waitWhileBusy { sentUserAgreement.get() == null || !submissionRequestReceived.get() }

    // then
    assertEquals(UserAgreement(ACCEPTED, DECLINED), sentUserAgreement.get())
    assertNull(SubmissionsManager.getInstance(project).getSubmissions(task)?.singleOrNull())
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
  
  private fun RecordedRequest.userAgreement(): UserAgreement? {
    val url = requestUrl ?: return null
    val pluginAgreement = url.queryParameter("pluginAgreement") ?: return null
    val aiAgreement = url.queryParameter("aiAgreement") ?: return null
    return UserAgreement(UserAgreementState.valueOf(pluginAgreement), UserAgreementState.valueOf(aiAgreement))
  }

  companion object {
    private const val SUBMISSION_ID = 5749695
    private const val SUBMISSION_RESPONSE = """{"id":$SUBMISSION_ID,"time":"2025-06-11T11:25:17.97224012"}"""
  }
}
