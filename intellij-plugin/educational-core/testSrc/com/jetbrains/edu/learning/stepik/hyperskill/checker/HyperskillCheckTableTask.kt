package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import org.apache.http.HttpStatus
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.util.*

class HyperskillCheckTableTask: HyperskillCheckActionTestBase() {
  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson {
          tableTask(
            stepId = 1,
            name = "table_task_correct",
            rows = listOf("A", "B"),
            columns = listOf("1", "2", "3"),
            selected = arrayOf(booleanArrayOf(false, true, false), booleanArrayOf(false, false, true)),
          ) {
            taskFile("Task.txt", "")
          }
          tableTask(
            stepId = 2,
            name = "table_task_incorrect",
            rows = listOf("A", "B"),
            columns = listOf("1", "2", "3"),
            selected = arrayOf(booleanArrayOf(false, false, false), booleanArrayOf(false, false, false)),
          ) {
            taskFile("Task.txt", "")
          }
        }
      }
    }
  }

  @Test
  fun `test table task correct`() = doTest(true, 0)

  @Test
  fun `test table task incorrect`() = doTest(false, 1)

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
          else -> error("Wrong path: $path")
        },
        responseCode = HttpStatus.SC_OK
      )
    }
  }

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
            "rows": [
              "A",
              "B"
            ],
            "columns": [
              "1",
              "2",
              "3"
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