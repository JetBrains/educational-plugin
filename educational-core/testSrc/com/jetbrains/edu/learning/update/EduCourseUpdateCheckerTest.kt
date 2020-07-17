package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.EduCourseUpdateChecker
import java.util.*

class EduCourseUpdateCheckerTest : CourseUpdateCheckerTestBase() {

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

  fun `test check scheduled for not upToDate course`() {
    doTestCheckScheduled(2, Date(0), false)
  }

  fun `test check scheduled for upToDate course`() {
    doTestCheckScheduled(2, Date(), true)
  }

  fun `test check scheduled for newly created course`() {
    doTestCheckScheduled(2, Date(), isCourseUpToDate = true, isNewlyCreated = true)
  }

  fun `test no isUpToDate check for newly created course at project opening`() {
    createCourse(Date(), isNewlyCreated = true)
    testNoCheck(EduCourseUpdateChecker.getInstance(project))
  }

  private fun doTestCheckScheduled(expectedInvocationNumber: Int,
                                   updateDate: Date,
                                   isCourseUpToDate: Boolean,
                                   isNewlyCreated: Boolean = false) {
    val course = createCourse(updateDate, isNewlyCreated)
    doTest(EduCourseUpdateChecker.getInstance(project), isCourseUpToDate, 0, expectedInvocationNumber) {
      assertEquals(isCourseUpToDate, course.isUpToDate)
    }
  }

  private fun createCourse(date: Date, isNewlyCreated: Boolean): EduCourse {
    val course = EduCourse().apply {
      name = "Test Course"
      id = 1
      updateDate = date
      isPublic = true
    }
    project.putUserData(EDU_PROJECT_CREATED, isNewlyCreated)
    StudyTaskManager.getInstance(project).course = course
    return course
  }

  override fun checkNotification(notificationListener: NotificationListener, isCourseUpToDate: Boolean) {
    assertEquals(notificationListener.notificationShown, !isCourseUpToDate)
    if (!isCourseUpToDate) {
      assertEquals(EduCoreBundle.message("update.content.request"), notificationListener.notificationText)
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/updateCourse/update_checker/"

  override fun tearDown() {
    try {
      val updateChecker = EduCourseUpdateChecker.getInstance(project)
      updateChecker.invocationNumber = 0
      updateChecker.cancelCheckRequests()
    }
    finally {
      super.tearDown()
    }
  }

  companion object {
    private val COURSES_REQUEST_RE = """/api/courses?.*""".toRegex()
  }
}