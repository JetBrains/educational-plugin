package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.update.CourseUpdateCheckerTestBase

class HyperskillCourseUpdateCheckerTest : CourseUpdateCheckerTestBase() {

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  private fun configureResponse(stagesResponse: String) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("course_response.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      STAGES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse(stagesResponse)
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      STEPS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("steps_response_111.json")
    }
  }

  fun `test check scheduled for upToDate course`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse()
    doTest(HyperskillCourseUpdateChecker.getInstance(project), true, 1, 2) {}
  }

  fun `test check scheduled for newly created course`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse(true)
    doTest(HyperskillCourseUpdateChecker.getInstance(project), true, 0, 1) {}
  }

  fun `test no isUpToDate check for newly created course at project opening`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse(true)
    testNoCheck(HyperskillCourseUpdateChecker.getInstance(project))
  }

  fun `test check scheduled for not upToDate course with notification`() {
    configureResponse("stages_response.json")
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 111) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, false)
    HyperskillSettings.INSTANCE.updateAutomatically = false

    doTest(HyperskillCourseUpdateChecker.getInstance(project), false, 1, 2, 2) {}
  }

  private fun createHyperskillCourse(isNewlyCreated: Boolean = false): HyperskillCourse {
    val course = HyperskillCourse().apply {
      name = "Test Course"
      id = 1
      project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, isNewlyCreated)
    }
    course.hyperskillProject = HyperskillProject()
    StudyTaskManager.getInstance(project).course = course
    return course
  }

  override fun checkNotification(notificationListener: NotificationListener,
                                 isCourseUpToDate: Boolean) {
    if (isCourseUpToDate) {
      if (notificationListener.notificationShown) {
        assertEquals(EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT),
                     notificationListener.notificationText)
      }
    }
    else {
      assertTrue("Notification wasn't shown", notificationListener.notificationShown)
      assertEquals(EduCoreBundle.message("update.content.request"), notificationListener.notificationText)
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "hyperskill/"

  override fun tearDown() {
    try {
      val updateChecker = HyperskillCourseUpdateChecker.getInstance(project)
      updateChecker.invocationNumber = 0
      updateChecker.cancelCheckRequests()
      HyperskillSettings.INSTANCE.updateAutomatically = true
    }
    finally {
      super.tearDown()
    }
  }

  companion object {
    private val COURSES_REQUEST_RE = """/api/projects?.*""".toRegex()
    private val STAGES_REQUEST_RE = """/api/stages?.*""".toRegex()
    private val STEPS_REQUEST_RE = """/api/steps?.*""".toRegex()
  }
}