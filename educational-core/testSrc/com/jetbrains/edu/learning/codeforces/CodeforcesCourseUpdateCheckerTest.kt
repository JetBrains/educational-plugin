package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.codeforces.CodeforcesCourseUpdateChecker.Companion.ONGOING_COURSE_CHECK_INTERVAL_SECONDS
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_PROBLEMS
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.MockCodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.update.CourseUpdateCheckerTestBase
import java.time.ZonedDateTime
import java.util.*

class CodeforcesCourseUpdateCheckerTest : CourseUpdateCheckerTestBase() {
  private val mockConnector: MockCodeforcesConnector get() = CodeforcesConnector.getInstance() as MockCodeforcesConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) {
      MockResponseFactory.fromFile(getTestFile("Contest 1211.html"))
    }
  }

  fun `test check scheduled for upToDate course`() {
    val course = createCodeforcesCourse()
    doTest(CodeforcesCourseUpdateChecker(project, course, testRootDisposable), true, 1, 2) {}
  }

  fun `test check scheduled for newly created course`() {
    val course = createCodeforcesCourse(isNewlyCreated = true)
    doTest(CodeforcesCourseUpdateChecker(project, course, testRootDisposable), true, 0, 1) {}
  }

  fun `test no isUpToDate check for newly created course at project opening`() {
    val course = createCodeforcesCourse(isNewlyCreated = true)
    testNoCheck(CodeforcesCourseUpdateChecker(project, course, testRootDisposable))
  }

  fun `test check scheduled for not upToDate course with notification`() {
    val taskName = "codeforcesTask"
    val course = courseWithFiles(
      courseProducer = ::CodeforcesCourse
    ) {
      lesson(CODEFORCES_PROBLEMS) {
        codeforcesTask(taskName)
      }
    } as CodeforcesCourse
    project.putUserData(EDU_PROJECT_CREATED, false)

    doTest(CodeforcesCourseUpdateChecker(project, course, testRootDisposable),
           false,
           1,
           2,
           2,
           notificationMessage = EduCoreBundle.message("codeforces.task.description.was.updated.notification", taskName)) {}
  }

  fun `test custom check interval for ongoing contest`() {
    val secondsToEnd = 3L
    val course = createCodeforcesCourse().apply {
      endDateTime = ZonedDateTime.now().plusSeconds(secondsToEnd)
    }
    assertTrue(course.isOngoing())

    val checker = CodeforcesCourseUpdateChecker(project, course, testRootDisposable)
    assertEquals(ONGOING_COURSE_CHECK_INTERVAL_SECONDS * DateFormatUtil.SECOND, checker.checkInterval)

    val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(secondsToEnd * 1000L) }
    EduUtils.waitAndDispatchInvocationEvents(future)

    checker.check()
    assertFalse(course.isOngoing())

    assertEquals(checker.getDefaultCheckInterval(), checker.checkInterval)
  }

  private fun createCodeforcesCourse(isNewlyCreated: Boolean = false): CodeforcesCourse = CodeforcesCourse().apply {
    name = "Test Course"
    id = 1
    updateDate = Date()
    project.putUserData(EDU_PROJECT_CREATED, isNewlyCreated)
  }

  override fun getTestDataPath(): String = "testData/codeforces/"

}