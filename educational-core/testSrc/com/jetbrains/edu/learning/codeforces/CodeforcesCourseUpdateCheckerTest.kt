package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_PROBLEMS
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.MockCodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.update.CourseUpdateCheckerTestBase
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
    val course = createCodeforcesCourse(Date(), isNewlyCreated = true)
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
           notificationMessage = EduCoreBundle.message("codeforces.task.updated.notification", taskName)) {}
  }

  private fun createCodeforcesCourse(date: Date = Date(), isNewlyCreated: Boolean = false): CodeforcesCourse = CodeforcesCourse().apply {
    name = "Test Course"
    id = 1
    updateDate = date
    project.putUserData(EDU_PROJECT_CREATED, isNewlyCreated)
  }

  override fun getTestDataPath(): String = "testData/codeforces/"

}