package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import okhttp3.mockwebserver.MockResponse
import org.junit.Test

class HyperskillCompleteTheoryTaskTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  @Test
  fun `test solving theory problem`() {
    logInFakeHyperskillUser()

    val stepId = 666
    var requestSent = false
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      if (request.pathWithoutPrams.endsWith("/api/steps/$stepId/complete")) {
        requestSent = true
        MockResponse().setResponseCode(200)
      }
      else {
        MockResponseFactory.notFound()
      }
    }

    val course = hyperskillCourseWithFiles(name = getProblemsProjectName("TEXT"), language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson") {
          theoryTask("theory", stepId = stepId) {
            taskFile("Task.txt", "file text")
            taskFile("task.html", "file text")
          }
          codeTask("task1") {
            taskFile("Task.txt", "file text")
            taskFile("task.html", "file text")
          }
          codeTask("task2") {
            taskFile("Task.txt", "file text")
            taskFile("task.html", "file text")
          }
        }
      }
    }

    val theoryTask = course.getProblem(stepId) ?: error("Can't find theory task")
    assertFalse(requestSent)
    assertFalse(theoryTask.status == CheckStatus.Solved)
    NavigationUtils.navigateToTask(project, theoryTask)
    assertTrue(requestSent)
    assertTrue(theoryTask.status == CheckStatus.Solved)
  }
}