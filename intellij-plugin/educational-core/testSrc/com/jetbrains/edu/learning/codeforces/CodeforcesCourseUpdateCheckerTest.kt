package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_PROBLEMS
import com.jetbrains.edu.learning.codeforces.CodeforcesTestCase.Companion.contest1211
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.MockCodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.parseResponseToAddContent
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker.Companion.ONGOING_COURSE_CHECK_INTERVAL
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.ContestParameters
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.Companion.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.update.CourseUpdateChecker
import com.jetbrains.edu.learning.update.CourseUpdateCheckerTestBase
import org.jsoup.Jsoup
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class CodeforcesCourseUpdateCheckerTest : CourseUpdateCheckerTestBase() {
  private val expectedTaskADescription: String by lazy {
    val doc = Jsoup.parse(loadText(contest1211))
    val course = CodeforcesCourse(ContestParameters(1211, KOTLIN, "48"))
    course.parseResponseToAddContent(doc)
    course.allTasks.first().descriptionText
  }
  private val mockConnector: MockCodeforcesConnector get() = CodeforcesConnector.getInstance() as MockCodeforcesConnector
  private val taskName: String = "codeforcesTask"

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  override fun checkNotification(notificationListener: NotificationListener, isCourseUpToDate: Boolean, notificationText: String) {
    super.checkNotification(notificationListener, isCourseUpToDate, EduCoreBundle.message("codeforces.task.description.was.updated.notification", taskName))
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { _, _ ->
      MockResponseFactory.fromFile(getTestFile(contest1211))
    }
  }

  @Test
  fun `test check scheduled for upToDate course`() {
    createCodeforcesCourse(isCourseUpToDate = true)
    doTest(CodeforcesCourseUpdateChecker.getInstance(project), true, 1, 2) {}
  }

  @Test
  fun `test check scheduled for newly created course`() {
    createCodeforcesCourse(isNewlyCreated = true, isCourseUpToDate = true)
    doTest(CodeforcesCourseUpdateChecker.getInstance(project), true, 0, 1) {}
  }

  @Test
  fun `test no isUpToDate check for newly created course at project opening`() {
    createCodeforcesCourse(isNewlyCreated = true)
    testNoCheck(CodeforcesCourseUpdateChecker.getInstance(project))
  }

  @Test
  fun `test check scheduled for not upToDate course with notification`() {
    createCodeforcesCourse()
    doTest(CodeforcesCourseUpdateChecker.getInstance(project), false, 1, 2, 2) {}
  }

  @Test
  fun `test custom check interval for ongoing contest`() {
    val secondsToEnd = 3L
    val course = createCodeforcesCourse().apply {
      startDate = ZonedDateTime.now().minusSeconds(secondsToEnd)
      endDateTime = ZonedDateTime.now().plusSeconds(secondsToEnd)
    }
    assertTrue(course.isOngoing)

    val checker = CodeforcesCourseUpdateChecker(project)
    assertEquals(ONGOING_COURSE_CHECK_INTERVAL, checker.checkInterval)

    val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(secondsToEnd * 1000L) }
    EduActionUtils.waitAndDispatchInvocationEvents(future)

    checker.check()
    assertFalse(course.isOngoing)
    assertEquals(TimeUnit.SECONDS.toMillis(Registry.intValue(CourseUpdateChecker.REGISTRY_KEY).toLong()), checker.checkInterval)
  }

  private fun createCodeforcesCourse(isNewlyCreated: Boolean = false, isCourseUpToDate: Boolean = false): CodeforcesCourse {
    val course = course(
      courseProducer = ::CodeforcesCourse
    ) {
      lesson(CODEFORCES_PROBLEMS) {
        codeforcesTask(taskName, taskDescription = if (isCourseUpToDate) expectedTaskADescription else null)
      }
    } as CodeforcesCourse
    course.apply {
      name = "Test Course"
      id = 1211
      updateDate = Date()
    }

    createCourseStructure(course)
    project.putUserData(EDU_PROJECT_CREATED, isNewlyCreated)
    return course
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "codeforces/"
}