package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.util.*

class HyperskillCheckChoiceTaskTest : HyperskillCheckActionTestBase() {

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("1_lesson_correct") {
          choiceTask(
            stepId = 1,
            name = "1_choice_task",
            isMultipleChoice = true,
            choiceOptions = mapOf(
              "Correct1" to ChoiceOptionStatus.UNKNOWN,
              "Incorrect" to ChoiceOptionStatus.UNKNOWN,
              "Correct2" to ChoiceOptionStatus.UNKNOWN
            ),
            selectedVariants = mutableListOf(0, 2)
          ) {
            taskFile("Task.txt", "")
          }
          choiceTask(
            stepId = 2,
            name = "2_choice_task",
            isMultipleChoice = false,
            choiceOptions = mapOf(
              "Correct1" to ChoiceOptionStatus.UNKNOWN,
              "Incorrect" to ChoiceOptionStatus.UNKNOWN,
              "Correct2" to ChoiceOptionStatus.UNKNOWN
            )
          ) {
            taskFile("Task.txt", "")
          }
        }
      }
    }
  }

  @Test
  fun `test choice task correct`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }
    val task = getCourse().allTasks[0]
    checkCheckAction(task, CheckStatus.Solved, "<html>Succeed solution</html>")
  }


  @Test
  fun `test choice task incorrect`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    val task = getCourse().allTasks[0]
    checkCheckAction(task, CheckStatus.Failed, "Wrong solution")
  }

  @Test
  fun `test choice task nothing selected`() {
    val task = getCourse().allTasks[1]
    checkCheckAction(task, CheckStatus.Failed, EduCoreBundle.message("choice.task.empty.variant"))
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
          "is_multiple_choice": true,
          "options": [
            "Correct1",
            "Incorrect",
            "Correct2" 
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