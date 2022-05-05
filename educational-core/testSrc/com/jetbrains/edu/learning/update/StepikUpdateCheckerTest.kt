package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.Companion.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.StepikUpdateChecker
import java.util.*

class StepikUpdateCheckerTest : CourseUpdateCheckerTestBase() {

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
    testNoCheck(StepikUpdateChecker.getInstance(project))
  }

  fun `test no check scheduled for marketplace course`() {
    createCourse(Date(), isNewlyCreated = false, isMarketplaceCourse = true)
    testNoCheck(StepikUpdateChecker.getInstance(project))
  }

  private fun doTestCheckScheduled(expectedInvocationNumber: Int,
                                   updateDate: Date,
                                   isCourseUpToDate: Boolean,
                                   isNewlyCreated: Boolean = false) {
    val course = createCourse(updateDate, isNewlyCreated)
    doTest(StepikUpdateChecker.getInstance(project), isCourseUpToDate, 0, expectedInvocationNumber, checkInterval = 1) {
      assertEquals(isCourseUpToDate, course.isUpToDate)
    }
  }

  private fun createCourse(date: Date, isNewlyCreated: Boolean, isMarketplaceCourse: Boolean = false): EduCourse {
    val course = course { } as EduCourse
    course.apply {
      name = "Test Course"
      id = 1
      updateDate = date
      isStepikPublic = true
      isMarketplace = isMarketplaceCourse
    }

    createCourseStructure(course)
    project.putUserData(EDU_PROJECT_CREATED, isNewlyCreated)
    return course
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "stepik/updateCourse/update_checker/"

  companion object {
    private val COURSES_REQUEST_RE = """/api/courses?.*""".toRegex()
  }
}