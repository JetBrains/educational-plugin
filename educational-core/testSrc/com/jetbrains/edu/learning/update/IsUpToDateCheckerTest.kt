package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.stepik.IsCourseUpToDateChecker
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import junit.framework.Assert
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
    doTest(2, Date(0), false)
  }

  fun `test check scheduled for upToDate course`() {
    doTest(2, Date(), true)
  }

  fun `test check scheduled for newly created course`() {
    doTest(2, Date(), isCourseUpToDate = true, isNewlyCreated = true)
  }

  fun `test no isUpToDate check for newly created course at project opening`() {
    val course = createCourse(Date(), isNewlyCreated = true)
    withCustomCheckInterval(2) {
      val isUpToDateChecker = IsCourseUpToDateChecker(course, project)
      isUpToDateChecker.check()
      val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(1000) }
      EduUtils.waitAndDispatchInvocationEvents(future)
      assertEquals(0, isUpToDateChecker.invocationNumber())
    }
  }

  @Suppress("DEPRECATION")
  private fun doTest(expectedInvocationNumber: Int, updateDate: Date, isCourseUpToDate: Boolean, isNewlyCreated: Boolean = false) {
    val course = createCourse(updateDate, isNewlyCreated)
    var notificationShown = false
    var notificationText = ""
    val connection = project.messageBus.connect(testRootDisposable)
    // BACKCOMPAT: 2019.2
    connection.subscribe(Notifications.TOPIC, object : NotificationsAdapter() {
      override fun notify(notification: Notification) {
        notificationShown = true
        notificationText = notification.content
      }
    })
    withCustomCheckInterval(1) {
      val isUpToDateChecker = IsCourseUpToDateChecker(course, project)
      isUpToDateChecker.check()
      assertEquals(0, isUpToDateChecker.invocationNumber())
      testCheckScheduled(expectedInvocationNumber, isUpToDateChecker)
      assertEquals(isCourseUpToDate, course.isUpToDate)
      if (isCourseUpToDate) {
        assertFalse("Notification was shown", notificationShown)
      }
      else {
        assertTrue("Notification wasn't shown", notificationShown)
        assertEquals(EduCoreBundle.message("update.course.popup.notification.content"), notificationText)
      }
      assertEquals(notificationShown, !isCourseUpToDate)
    }
  }

  private fun testCheckScheduled(expectedInvocationNumber: Int, isUpToDateChecker: IsCourseUpToDateChecker) {
    val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(3000) }
    EduUtils.waitAndDispatchInvocationEvents(future)
    check(expectedInvocationNumber <= isUpToDateChecker.invocationNumber())
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

  private fun createCourse(date: Date, isNewlyCreated: Boolean): EduCourse = EduCourse().apply {
    name = "Test Course"
    id = 1
    updateDate = date
    if (isNewlyCreated) {
      project.putUserData(EDU_PROJECT_CREATED, true)
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/updateCourse/update_checker/"

  private fun getTestFile(fileName: String) = testDataPath + fileName

  companion object {
    private val COURSES_REQUEST_RE = """/api/courses?.*""".toRegex()
  }
}