package com.jetbrains.edu.learning.marketplace

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.update.CourseUpdateCheckerTestBase
import org.junit.Test

class MarketplaceUpdateCheckerTest : CourseUpdateCheckerTestBase() {

  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      UPDATE_INFO_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      mockResponse("updateInfo.json")
    }
  }

  @Test
  fun `test check scheduled for not upToDate course`() {
    doTestCheckScheduled(expectedInvocationNumber = 2, isCourseUpToDate = false, courseVersion = 1)
  }

  @Test
  fun `test check scheduled for upToDate course`() {
    doTestCheckScheduled(expectedInvocationNumber = 2, isCourseUpToDate = true)
  }

  @Test
  fun `test check scheduled for newly created course`() {
    doTestCheckScheduled(expectedInvocationNumber = 2, isCourseUpToDate = true, isNewlyCreated = true)
  }

  @Test
  fun `test isUpToDate check invoked for newly created course at project opening`() {
    createCourse(isNewlyCreated = true, courseVersion = 3)
    val updateChecker = MarketplaceUpdateChecker.getInstance(project)

    updateChecker.check()
    repeat(10) {
      Thread.sleep(50)
      UIUtil.dispatchAllInvocationEvents()
      if (updateChecker.invocationNumber >= 1) {
        return
      }
    }

    fail("Tired of waiting for update checker invoked")
  }

  @Test
  fun `test no check scheduled for stepik course`() {
    createCourse(isNewlyCreated = false, courseVersion = 0, isMarketplaceCourse = false)
    testNoCheck(MarketplaceUpdateChecker.getInstance(project))
  }

  private fun doTestCheckScheduled(expectedInvocationNumber: Int,
                                   isCourseUpToDate: Boolean,
                                   courseVersion: Int = 3,
                                   isNewlyCreated: Boolean = false) {
    val course = createCourse(isNewlyCreated, courseVersion)
    doTest(MarketplaceUpdateChecker.getInstance(project), isCourseUpToDate, 0, expectedInvocationNumber, checkInterval = 1) {
      assertEquals(isCourseUpToDate, course.isUpToDate)
    }
  }

  private fun createCourse(isNewlyCreated: Boolean, courseVersion: Int, isMarketplaceCourse: Boolean = true): EduCourse {
    val course = course { } as EduCourse
    with(course) {
      name = "Test Course"
      id = 1
      marketplaceCourseVersion = courseVersion
      isMarketplace = isMarketplaceCourse
    }

    createCourseStructure(course)
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, isNewlyCreated)
    return course
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "marketplace/updateInfo/"

  companion object {
    private val UPDATE_INFO_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}