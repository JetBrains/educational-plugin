package com.jetbrains.edu.learning.stepik.hyperskill.checker.retry

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.RetryAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillActionTestBase
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.testAction
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.util.*

class HyperskillRetryActionTest : HyperskillActionTestBase() {

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("1_lesson_correct") {
          choiceTask(
            stepId = 5545,
            name = "5545_choice_task",
            isMultipleChoice = true,
            choiceOptions = mapOf(
              "2" to ChoiceOptionStatus.UNKNOWN,
              "1" to ChoiceOptionStatus.UNKNOWN,
              "0" to ChoiceOptionStatus.UNKNOWN
            ),
            status = CheckStatus.Failed
          ) {
            taskFile("Task.txt", "")
          }
          eduTask(
            stepId = 2,
            name = "2_edu_task"
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
          else -> error("Wrong path: $path")
        }
      )
    }
    val task = getCourse().allTasks[0] as ChoiceTask

    checkRetryAction(task)

    assertEquals("Name for ${task.name} doesn't match", "5545_choice_task", task.name)
    assertEquals("Status for ${task.name} doesn't match", CheckStatus.Unchecked, task.status)
    assertTrue("isMultipleChoice for ${task.name} doesn't match", task.isMultipleChoice)
    assertTrue("choiceOptions for ${task.name} doesn't match", task.selectedVariants.isEmpty())
    assertEquals("choiceOptions for ${task.name} doesn't match", mutableListOf("0", "1", "2"), task.choiceOptions.map { it.text })

  }

  @Test
  fun `test is not changed on failed task`() {
    checkRetryAction(getCourse().allTasks[1])
  }

  @Test
  fun `test empty dataset`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/attempts" -> emptyDatasetAttempt
          else -> error("Wrong path: $path")
        }
      )
    }

    val task = getCourse().allTasks[0] as ChoiceTask
    checkRetryAction(task)
    assertEquals("choiceOptions for ${task.name} doesn't match", mutableListOf("2", "1", "0"), task.choiceOptions.map { it.text })
  }

  private fun checkRetryAction(task: Task) {
    NavigationUtils.navigateToTask(project, task)
    testAction(RetryAction.ACTION_ID)
  }

  @Language("JSON")
  private val attempt = """
    {
      "meta" : {
        "page" : 1,
        "has_next" : false,
        "has_previous" : false
      },
      "attempts" : [
        {
          "dataset" : {
            "is_multiple_choice" : true,
            "options" : [
              "0",
              "1",
              "2"
            ]
          },
          "id" : 48510847,
          "status" : "active",
          "step" : 5545,
          "time": "${Date().format()}",
          "user" : 1,
          "time_left" : null
        }
      ]
    }
  """

  @Language("JSON")
  private val emptyDatasetAttempt = """
    {
      "meta" : {
        "page" : 1,
        "has_next" : false,
        "has_previous" : false
      },
      "attempts" : [
        {
          "id" : 48510847,
          "status" : "active",
          "step" : 5545,
          "time": "${Date().format()}",
          "user" : 1,
          "time_left" : null
        }
      ]
    }
  """

}