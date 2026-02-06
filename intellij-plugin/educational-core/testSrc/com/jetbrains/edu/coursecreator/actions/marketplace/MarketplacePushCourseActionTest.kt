package com.jetbrains.edu.coursecreator.actions.marketplace

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.notification.NotificationType
import com.intellij.util.application
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.marketplace.api.*
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import io.mockk.every
import io.mockk.verify
import kotlin.test.Test

// TODO: cover more cases
// TODO: refactor MarketplaceConnector and decouple business logic from network requests.
//  It will allow us to stop using mock web server in tests and check business logic only
class MarketplacePushCourseActionTest : EduActionTestCase() {

  private val mapper = jacksonObjectMapper()

  private lateinit var mockConnector: MockMarketplaceConnector

  override fun setUp() {
    super.setUp()
    val mockMarketplaceSettings = mockService<MarketplaceSettings>(application)
    every { mockMarketplaceSettings.getMarketplaceAccount() } returns MarketplaceAccount(userInfo)
    mockConnector = mockService<MarketplaceConnector>(application) as MockMarketplaceConnector
    every { mockConnector.loadHubToken() } returns HUB_TOKEN
  }

  @Test
  fun `test new course uploads successfully`() {
    // given
    val course = createEduCourse()

    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      when (path) {
        "/api/plugins/edu/upload/details" -> MockResponseFactory.fromString(mapper.writeValueAsString(successUploadResponse))
        else -> null
      }
    }

    // when
    val notification = runAndWaitForNotification(project) {
      testAction(MarketplacePushCourse.ACTION_ID)
    }

    // then
    assertEquals(NotificationType.INFORMATION, notification.type)
    assertEquals(COURSE_ID, course.id)
    assertEquals(Vendor(FULL_USER_NAME), course.vendor)

    verify(exactly = 1) { mockConnector.loadHubToken() }
    verify(exactly = 1) { mockConnector.uploadNewCourseUnderProgress(project, HUB_TOKEN, match { it.course == course }) }
  }

  @Test
  fun `test new course uploading fails`() {
    // given
    val course = createEduCourse()

    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      when (path) {
        "/api/plugins/edu/upload/details" -> MockResponseFactory.badRequest()
        else -> null
      }
    }

    // when
    val notification = runAndWaitForNotification(project) {
      testAction(MarketplacePushCourse.ACTION_ID)
    }

    // then
    assertEquals(NotificationType.ERROR, notification.type)
    assertEquals(0, course.id)
    assertEquals(Vendor(FULL_USER_NAME), course.vendor)

    verify(exactly = 1) { mockConnector.loadHubToken() }
    verify(exactly = 1) { mockConnector.uploadNewCourseUnderProgress(project, HUB_TOKEN, match { it.course == course }) }
  }

  private fun createEduCourse(): EduCourse {
    return courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      id = 0,
      createYamlConfigs = true
    ) {
      lesson("lesson1") {
        eduTask("task1") { taskFile("Task.txt", "content") }
      }
    }.apply {
      isMarketplace = true
    } as EduCourse
  }

  companion object {
    private const val HUB_TOKEN = "test-token"
    private const val COURSE_ID = 1
    private const val FULL_USER_NAME = "Foo Bar"

    private val successUploadResponse = SuccessCourseUploadResponse(
      emptyList(),
      CourseBean(id = COURSE_ID, name = "TestCourse")
    )

    private val userInfo = JBAccountUserInfo().apply {
      name = FULL_USER_NAME
      jbaLogin = "foobar"
      email = "foobar@example.com"
    }
  }
}
