package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.apache.http.HttpStatus
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.util.*

class HyperskillCheckSortingBasedTaskTest : HyperskillCheckActionTestBase() {

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("1_lesson_correct") {
          sortingTask(
            stepId = 1,
            name = "sorting_task_correct",
            options = listOf("1", "2", "0"),
            ordering = intArrayOf(1, 2, 0)
          ) {
            taskFile("Task.txt", "")
          }
          sortingTask(
            stepId = 2,
            name = "sorting_task_incorrect",
            options = listOf("1", "2", "0"),
            ordering = intArrayOf(1, 0, 2)
          ) {
            taskFile("Task.txt", "")
          }
          matchingTask(
            stepId = 3,
            name = "matching_task_correct",
            captions = listOf("A", "B", "C"),
            options = listOf("1", "2", "0"),
            ordering = intArrayOf(1, 2, 0)
          ) {
            taskFile("Task.txt", "")
          }
          matchingTask(
            stepId = 4,
            name = "matching_task_incorrect",
            captions = listOf("A", "B", "C"),
            options = listOf("1", "2", "0"),
            ordering = intArrayOf(1, 0, 2)
          ) {
            taskFile("Task.txt", "")
          }
        }
      }
    }
  }

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
        responseCode = HttpStatus.SC_OK
      )
    }
  }

  private fun doTest(succeed: Boolean, taskNum: Int) {
    val task = getCourse().allTasks[taskNum]
    configureResponses(succeed)
    if (succeed) {
      checkCheckAction(task, CheckStatus.Solved, "<html>Succeed solution</html>")
    }
    else {
      checkCheckAction(task, CheckStatus.Failed, "Wrong solution")
    }
  }

  @Test
  fun `test sorting task correct`() = doTest(true, 0)

  @Test
  fun `test sorting task incorrect`() = doTest(false, 1)

  @Test
  fun `test matching task correct`() = doTest(true, 2)

  @Test
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