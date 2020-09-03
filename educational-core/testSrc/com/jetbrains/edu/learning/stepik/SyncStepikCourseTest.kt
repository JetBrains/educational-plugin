package com.jetbrains.edu.learning.stepik

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.SyncStepikCourseAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import java.util.*

class SyncStepikCourseTest : EduTestCase() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("remote_course.json")
    }
  }

  fun `test course updated`() {
    val course = createCourse(Date(0))
    myFixture.testAction(SyncStepikCourseAction())
    assertEquals("Test Course", course.description)
    assertTrue("Updated course should be up to date", course.isUpToDate)
  }

  fun `test notification shown for up to date course`() {
    createCourse(Date())

    var notificationShown = false
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        notificationShown = true
        assertEquals(EduCoreBundle.message("stepik.course.up.to.date"), notification.title)
      }
    })

    myFixture.testAction(SyncStepikCourseAction())
    assertTrue(notificationShown)
  }

  private fun createCourse(date: Date): EduCourse {
    val course = EduCourse().apply {
      name = "Test Course"
      id = 1
      updateDate = date
      description = "Outdated Description"
      isPublic = true
      isUpToDate = true
    }
    StudyTaskManager.getInstance(project).course = course
    return course
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/updateCourse/update_checker/"

  companion object {
    private val COURSES_REQUEST_RE = """/api/courses?.*""".toRegex()
  }
}