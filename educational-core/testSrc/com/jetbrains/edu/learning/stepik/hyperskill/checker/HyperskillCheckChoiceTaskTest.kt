package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import com.jetbrains.edu.learning.testAction
import org.intellij.lang.annotations.Language
import java.util.*

class HyperskillCheckChoiceTaskTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("1_lesson_correct") {
          choiceTask(stepId = 1,
                     name = "1_choice_task",
                     isMultipleChoice = true,
                     choiceOptions = mapOf("Correct1" to ChoiceOptionStatus.UNKNOWN,
                                           "Incorrect" to ChoiceOptionStatus.UNKNOWN,
                                           "Correct2" to ChoiceOptionStatus.UNKNOWN),
                     selectedVariants = mutableListOf(0, 2)) {
            taskFile("Task.txt", "")
          }
          choiceTask(stepId = 2,
                     name = "2_choice_task",
                     isMultipleChoice = false,
                     choiceOptions = mapOf("Correct1" to ChoiceOptionStatus.UNKNOWN,
                                           "Incorrect" to ChoiceOptionStatus.UNKNOWN,
                                           "Correct2" to ChoiceOptionStatus.UNKNOWN)) {
            taskFile("Task.txt", "")
          }
        }
      }
    }
  }

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    logOutFakeHyperskillUser()
    super.tearDown()
  }

  fun `test choice task correct`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts?step=1&user=1" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }
    CheckActionListener.reset()
    CheckActionListener.expectedMessage { "<html>Succeed solution</html>" }
    val course = getCourse()
    NavigationUtils.navigateToTask(project, course.allTasks[0])
    testAction(CheckAction.ACTION_ID)
  }


  fun `test choice task incorrect`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts?step=1&user=1" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Wrong solution" }
    val course = getCourse()
    NavigationUtils.navigateToTask(project, course.allTasks[0])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test choice task nothing selected `() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { EduCoreBundle.message("choice.task.empty.variant") }
    val course = getCourse()
    NavigationUtils.navigateToTask(project, course.allTasks[1])
    testAction(CheckAction.ACTION_ID)
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