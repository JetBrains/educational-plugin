package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.StepikTestUtils.logOutFakeStepikUser
import com.jetbrains.edu.learning.stepik.StepikTestUtils.loginFakeStepikUser
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.http.HttpStatus

class StepikSubmissionsTest : SubmissionsTestBase() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
    loginFakeStepikUser()
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task1", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    } as EduCourse
  }

  override fun tearDown() {
    logOutFakeStepikUser()
    super.tearDown()
  }

  private fun configureResponse(items: Map<Int, String>) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val result = SUBMISSIONS_GET_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      val stepId = result.groupValues[1].toInt()
      items[stepId]?.let { mockResponse(it) } ?: mockResponse("submissions_response_empty.json")
    }
  }

  fun `test edu task submissions loaded`() {
    configureResponse(mapOf(1 to "submissions_response_1.json"))
    doTestSubmissionsLoaded(setOf(1), mapOf(1 to 1))
  }

  fun `test edu task several submissions loaded`() {
    configureResponse(mapOf(1 to "submissions_response_3.json"))
    doTestSubmissionsLoaded(setOf(1), mapOf(1 to 2))
  }

  fun `test several edu tasks submissions loaded`() {
    configureResponse(mapOf(1 to "submissions_response_3.json", 2 to "submissions_response_2.json"))
    doTestSubmissionsLoaded(setOf(1, 2), mapOf(1 to 2, 2 to 1))
  }

  fun `test submission added after edu task check`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      ATTEMPTS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("attempts.json", HttpStatus.SC_CREATED)
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      SUBMISSIONS_POST_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("submissions_response_2.json", HttpStatus.SC_CREATED)
    }

    doTestSubmissionAddedAfterTaskCheck(1, EduNames.CORRECT)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/loadSolutions/"

  companion object {
    private val SUBMISSIONS_GET_REQUEST_RE = """/api/submissions?.*step=(\d*).*""".toRegex()
    private val SUBMISSIONS_POST_REQUEST_RE = """/api/submissions?.*""".toRegex()
    private val ATTEMPTS_REQUEST_RE = """/api/attempts?.*""".toRegex()
  }
}