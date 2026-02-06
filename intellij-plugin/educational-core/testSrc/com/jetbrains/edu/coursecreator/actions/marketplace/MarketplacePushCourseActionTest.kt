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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import java.util.UUID
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
  fun `test new course uploads successfully with empty local vendor`() {
    // given
    val course = createEduCourse()

    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns listOf(personalOrganization)

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
    coVerify(exactly = 1) { mockConnector.loadUserOrganizations(HUB_TOKEN) }
    verify(exactly = 1) {
      mockConnector.uploadNewCourseUnderProgress(
        project,
        HUB_TOKEN,
        match { it.course == course && it.organization == personalOrganization }
      )
    }
  }

  @Test
  fun `test new course uploading fails`() {
    // given
    val course = createEduCourse()

    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns listOf(personalOrganization)

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
    verify(exactly = 1) {
      mockConnector.uploadNewCourseUnderProgress(
        project,
        HUB_TOKEN,
        match { it.course == course && it.organization == personalOrganization }
      )
    }
  }

  @Test
  fun `test vendor identification fails when user has no organizations`() {
    // given
    val course = createEduCourse(Vendor(FULL_USER_NAME))

    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns emptyList()

    // when
    val notification = runAndWaitForNotification(project) {
      testAction(MarketplacePushCourse.ACTION_ID)
    }

    // then
    assertEquals(NotificationType.ERROR, notification.type)
    assertEquals(0, course.id)
    assertEquals(Vendor(FULL_USER_NAME), course.vendor)

    verify(exactly = 1) { mockConnector.loadHubToken() }
    coVerify(exactly = 1) { mockConnector.loadUserOrganizations(HUB_TOKEN) }
    verify(exactly = 0) { mockConnector.uploadNewCourseUnderProgress(any(), any(), any()) }
  }

  @Test
  fun `test new course uploads successfully with non-empty local vendor`() {
    // given
    val course = createEduCourse(vendor = Vendor(FULL_USER_NAME))

    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns listOf(personalOrganization)

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
    coVerify(exactly = 1) { mockConnector.loadUserOrganizations(HUB_TOKEN) }
    verify(exactly = 1) {
      mockConnector.uploadNewCourseUnderProgress(
        project,
        HUB_TOKEN,
        match { it.course == course && it.organization == personalOrganization }
      )
    }
  }

  @Test
  fun `test new course uploads successfully with non-empty local vendor and several organizations`() {
    // given
    val course = createEduCourse(vendor = Vendor(FULL_USER_NAME))

    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns listOf(organization, personalOrganization)

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
    coVerify(exactly = 1) { mockConnector.loadUserOrganizations(HUB_TOKEN) }
    verify(exactly = 1) {
      mockConnector.uploadNewCourseUnderProgress(
        project,
        HUB_TOKEN,
        match { it.course == course && it.organization == personalOrganization }
      )
    }
  }


  @Test
  fun `test vendor identification fails when local vendor name does not match any organization`() {
    // given
    val unknownVendor = Vendor("Some Unknown Vendor")
    val course = createEduCourse(unknownVendor)

    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns listOf(personalOrganization, organization)

    // when
    val notification = runAndWaitForNotification(project) {
      testAction(MarketplacePushCourse.ACTION_ID)
    }

    // then
    assertEquals(NotificationType.ERROR, notification.type)
    assertEquals(0, course.id)
    assertEquals(unknownVendor, course.vendor)

    verify(exactly = 1) { mockConnector.loadHubToken() }
    coVerify(exactly = 1) { mockConnector.loadUserOrganizations(HUB_TOKEN) }
    verify(exactly = 0) { mockConnector.uploadNewCourseUnderProgress(any(), any(), any()) }
  }

  @Test
  fun `test vendor is updated with organization data on successful upload`() {
    // given
    val course = createEduCourse(vendor = Vendor(ORGANIZATION_NAME))

    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns listOf(organization)

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
    assertEquals(Vendor(ORGANIZATION_PUBLIC_NAME, ORGANIZATION_EMAIL, ORGANIZATION_URL), course.vendor)

    verify(exactly = 1) { mockConnector.loadHubToken() }
    coVerify(exactly = 1) { mockConnector.loadUserOrganizations(HUB_TOKEN) }
    verify(exactly = 1) {
      mockConnector.uploadNewCourseUnderProgress(
        project,
        HUB_TOKEN,
        match { it.course == course && it.organization == organization }
      )
    }
  }

  @Test
  fun `test vendor email is not set when organization showEmail is false`() {
    // given
    val course = createEduCourse(vendor = Vendor(ORGANIZATION_NAME))

    val organization = organization.copy(showEmail = false)
    coEvery { mockConnector.loadUserOrganizations(HUB_TOKEN) } returns listOf(organization)

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
    assertEquals(Vendor(ORGANIZATION_PUBLIC_NAME, null, ORGANIZATION_URL), course.vendor)

    verify(exactly = 1) { mockConnector.loadHubToken() }
    coVerify(exactly = 1) { mockConnector.loadUserOrganizations(HUB_TOKEN) }
    verify(exactly = 1) {
      mockConnector.uploadNewCourseUnderProgress(
        project,
        HUB_TOKEN,
        match { it.course == course && it.organization == organization }
      )
    }
  }

  private fun createEduCourse(vendor: Vendor? = null): EduCourse {
    return courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      id = 0,
      createYamlConfigs = true,
      courseVendor = vendor
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
    private const val EMAIL = "foobar@example.com"

    private const val ORGANIZATION_NAME = "Qwerty"
    private const val ORGANIZATION_PUBLIC_NAME = "Qwerty Ltd."
    private const val ORGANIZATION_EMAIL = "qwerty@example.com"
    private const val ORGANIZATION_URL = "https://qwerty.example.com"

    private val successUploadResponse = SuccessCourseUploadResponse(
      emptyList(),
      CourseBean(id = COURSE_ID, name = "TestCourse")
    )

    private val userInfo = JBAccountUserInfo().apply {
      name = FULL_USER_NAME
      jbaLogin = "foobar"
      email = EMAIL
    }

    private val personalOrganization = UserOrganization(
      name = UUID.randomUUID().toString(),
      publicName = FULL_USER_NAME,
      email = EMAIL,
      showEmail = false,
      url = null
    )

    private val organization = UserOrganization(
      name = ORGANIZATION_NAME,
      publicName = ORGANIZATION_PUBLIC_NAME,
      email = ORGANIZATION_EMAIL,
      showEmail = true,
      url = ORGANIZATION_URL
    )
  }
}
