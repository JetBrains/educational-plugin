package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.IsCourseUpToDateChecker
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import java.util.*

class IsUpToDateCheckerTest : EduTestCase() {

  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      MockResponseFactory.fromFile(getTestFile("remote_course.json"))
    }
  }

  fun `test check scheduled for not upToDate course`() {
    withCustomCheckInterval(1) { doTest(Date(0), false) }
  }

  fun `test check scheduled for upToDate course`() {
    withCustomCheckInterval(1) { doTest(Date(), true) }
  }

  private fun doTest(updateDate: Date, isCourseUpToDate: Boolean) {
    val course = createCourse(updateDate)
    var notificationShown = false
    var notificationText = ""
    val connection = project.messageBus.connect(testRootDisposable)
    // BACKOMPAT: 2019.2
    connection.subscribe(Notifications.TOPIC, object : NotificationsAdapter() {
      override fun notify(notification: Notification) {
        notificationShown = true
        notificationText = notification.content
      }
    })
    val isUpToDateChecker = IsCourseUpToDateChecker(course, project)
    isUpToDateChecker.check()
    assertEquals(0, isUpToDateChecker.invocationNumber())
    Thread.sleep(1500)
    UIUtil.dispatchAllInvocationEvents()
    assertEquals(isCourseUpToDate, course.isUpToDate)
    assertEquals(1, isUpToDateChecker.invocationNumber())
    if (isCourseUpToDate) {
      assertFalse("Notification was shown", notificationShown)
    }
    else {
      assertTrue("Notification wasn't shown", notificationShown)
      assertEquals(EduCoreBundle.message("update.course.popup.notification.content"), notificationText)
    }
    assertEquals(notificationShown, !isCourseUpToDate)
  }

  private fun withCustomCheckInterval(interval: Int, action: () -> Unit) {
    val registryValue = Registry.get(IsCourseUpToDateChecker.REGISTRY_KEY)
    val oldValue = registryValue.asInteger()
    registryValue.setValue(interval)
    try {
      action()
    }
    finally {
      registryValue.setValue(oldValue)
    }
  }

  private fun createCourse(date: Date): EduCourse = EduCourse().apply {
    name = "Test Course"
    id = 1
    updateDate = date
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/updateCourse/update_checker/"

  private fun getTestFile(fileName: String) = testDataPath + fileName

  companion object {
    private val COURSES_REQUEST_RE = """/api/courses?.*""".toRegex()
  }
}