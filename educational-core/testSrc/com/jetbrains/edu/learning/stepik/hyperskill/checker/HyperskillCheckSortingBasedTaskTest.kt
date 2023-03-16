package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.PlaintTextCheckerFixture
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import org.apache.http.HttpStatus
import org.intellij.lang.annotations.Language
import java.util.*

class HyperskillCheckSortingBasedTaskTest : CheckersTestBase<Unit>() {
  private val defaultResponseCode: Int = HttpStatus.SC_OK

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    logOutFakeHyperskillUser()
    super.tearDown()
  }

  override fun createCourse(): Course = course(courseProducer = ::HyperskillCourse) {
    section("Topics") {
      lesson("1_lesson_correct") {
        sortingTask(stepId = 1,
          name = "sorting_task_correct",
          options = listOf("1", "2", "0"),
          ordering = intArrayOf(1, 2, 0)
        ) {
          taskFile("Task.txt", "")
        }
        sortingTask(stepId = 2,
          name = "sorting_task_incorrect",
          options = listOf("1", "2", "0"),
          ordering = intArrayOf(1, 0, 2)
        ) {
          taskFile("Task.txt", "")
        }
        matchingTask(stepId = 3,
          name = "matching_task_correct",
          captions = listOf("A", "B", "C"),
          options = listOf("1", "2", "0"),
          ordering = intArrayOf(1, 2, 0)
        ) {
          taskFile("Task.txt", "")
        }
        matchingTask(stepId = 4,
          name = "matching_task_incorrect",
          captions = listOf("A", "B", "C"),
          options = listOf("1", "2", "0"),
          ordering = intArrayOf(1, 0, 2)
        ) {
          taskFile("Task.txt", "")
        }
      }
    }
  } as HyperskillCourse

  private fun configureResponses(succeed: Boolean) {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> if (succeed) {
            submissionWithSucceedStatus
          }
          else {
            submissionWithFailedStatus
          }
          else -> error("Wrong path: ${path}")
        },
        responseCode = defaultResponseCode
      )
    }
  }

  private fun doTest(succeed: Boolean, taskNum: Int) {
    if (succeed) {
      CheckActionListener.reset()
      CheckActionListener.expectedMessage { "<html>Succeed solution</html>" }
    } else {
      CheckActionListener.shouldFail()
      CheckActionListener.expectedMessage { "Wrong solution" }
    }
    val task = myCourse.allTasks[taskNum]
    configureResponses(succeed)
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }

  fun `test sorting task correct`() = doTest(true, 0)

  fun `test sorting task incorrect`() = doTest(false, 1)

  fun `test matching task correct`() = doTest(true, 2)

  fun `test matching task incorrect`() = doTest(false, 3)

  @Language("JSON")
  private val attempt = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": {
          "options": [
            "1",
            "2",
            "0" 
            ]
          },
          "id": 102,
          "status": "active",
          "step": 1,
          "time": "${Date().format()}",
          "time_left": null,
          "user": 1
        }
      ]
    }
  """

  @Language("JSON")
  private val submission = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "11",
          "id": "11",
          "status": "evaluation",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1
        }
      ]
    }
  """

  @Language("JSON")
  private val submissionWithSucceedStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "11",
          "id": "11",
          "status": "succeed",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1
        }
      ]
    }
  """

  @Language("JSON")
  private val submissionWithFailedStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "11",
          "id": "11",
          "status": "wrong",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1
        }
      ]
    }
  """
}