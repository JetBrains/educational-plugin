package com.jetbrains.edu.lti

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.util.application
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import io.mockk.*
import org.junit.Test

class LtiGradingErrorNotificationTest : EduActionTestCase() {

  // successful posting

  @Test
  fun `test successful post with lms != null`() = doTest(
    courseLink = null,
    lmsDescription = "Test LMS",
    serverStatus = Success,

    expectedNotificationType = NotificationType.INFORMATION,
    expectedNotificationText = "Your task completion status has been posted to the grading table of 'Test LMS'."
  )

  @Test
  fun `test success with lms == null`() = doTest(
    courseLink = null,
    lmsDescription = null,
    serverStatus = Success,

    expectedNotificationType = NotificationType.INFORMATION,
    expectedNotificationText = "Your task completion status has been posted to the grading table."
  )

  // posting with some unknown server error

  @Test
  fun `test server error with lms != null, link != null`() = doTest(
    courseLink = "https-link-to-course",
    lmsDescription = "Test LMS",
    serverStatus = ServerError,

    expectedNotificationText = """
      Failed to post your task completion status to the online platform.
      Try opening this task in the browser, then navigate back to <b>${ApplicationNamesInfo.getInstance().fullProductName}</b> by clicking the "Start Coding" button.
      Your online course: <a href="https-link-to-course">Test LMS</a>
    """
  )

  @Test
  fun `test server error with lms == null, link != null`() = doTest(
    courseLink = "https-link-to-course",
    lmsDescription = null,
    serverStatus = ServerError,

    expectedNotificationText = """
      Failed to post your task completion status to the online platform.
      Try opening this task in the browser, then navigate back to <b>${ApplicationNamesInfo.getInstance().fullProductName}</b> by clicking the "Start Coding" button.
      Your online course: <a href="https-link-to-course">https-link-to-course</a>
    """
  )

  @Test
  fun `test server error with lms != null, link == null`() = doTest(
    courseLink = null,
    lmsDescription = "Test LMS",
    serverStatus = ServerError,

    expectedNotificationText = """
      Failed to post your task completion status to the online platform.
      Try opening this task in the browser, then navigate back to <b>${ApplicationNamesInfo.getInstance().fullProductName}</b> by clicking the "Start Coding" button.
      Your online course: Test LMS
    """
  )

  @Test
  fun `test server error with lms == null, link == null`() = doTest(
    courseLink = null,
    lmsDescription = null,
    serverStatus = ServerError,

    expectedNotificationText = """
      Failed to post your task completion status to the online platform.
      Try opening this task in the browser, then navigate back to <b>${ApplicationNamesInfo.getInstance().fullProductName}</b> by clicking the "Start Coding" button.
    """
  )

  // other specific server errors

  @Test
  fun `test unknown launch id`() = doTest(
    courseLink = "https-course-link",
    lmsDescription = "Test LMS",
    serverStatus = UnknownLaunchId,

    expectedNotificationText = """
      Failed to post your task completion status to the online platform.
      Error message: unknown launch id
      Your online course: <a href="https-course-link">Test LMS</a>
    """
  )

  @Test
  fun `test connection error`() = doTest(
    courseLink = "https-course-link",
    lmsDescription = "Test LMS",
    serverStatus = ConnectionError("not allowed to view this page"),

    expectedNotificationText = """
      Failed to post your task completion status to the online platform.
      Error message: not allowed to view this page
      Your online course: <a href="https-course-link">Test LMS</a>
    """
  )

  fun doTest(
    courseLink: String?,
    lmsDescription: String?,
    serverStatus: PostTaskSolvedStatus,

    expectedNotificationType: NotificationType = NotificationType.ERROR,
    expectedNotificationText: String
  ) {
    // given
    val course = createEduCourse()
    val task = course.allTasks[0]

    val ltiSettingsManager = mockService<LTISettingsManager>(project)
    val ltiConnector = mockService<LTIConnector>(application)
    mockkObject(EduNotificationManager)

    every { ltiConnector.postTaskChecked(any(), any(), any(), any(), any()) } returns serverStatus
    every { ltiSettingsManager.settings } returns LTISettingsDTO(
      "launch-id-238",
      lmsDescription,
      LTIOnlineService.STANDALONE,
      courseLink
    )

    val notificationTypeSlot = slot<NotificationType>()
    val notificationTextSlot = slot<String>()
    every { EduNotificationManager.create(capture(notificationTypeSlot), any(), capture(notificationTextSlot)) } returns mockk(relaxed = true)

    // when
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    verify(exactly = 1) { EduNotificationManager.create(any(), any(), any()) }

    assertEquals(
      "Notification type is not as expected",
      expectedNotificationType,
      notificationTypeSlot.captured
    )

    assertEquals(
      "Notification text is not as expected",
      expectedNotificationText.trimIndent(),
      notificationTextSlot.captured.split("<br>").joinToString("\n")
    )
  }

  private fun createEduCourse(): Course {
    val course = courseWithFiles(id = 238, courseMode = CourseMode.STUDENT) {
      lesson {
        eduTask {
          taskFile("task.txt")
        }
      }
    }
    course.isMarketplace = true
    return course
  }
}