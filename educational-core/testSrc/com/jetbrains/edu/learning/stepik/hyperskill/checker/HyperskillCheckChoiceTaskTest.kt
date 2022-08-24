package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.checker.PlaintTextCheckerFixture
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getPathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import org.intellij.lang.annotations.Language
import java.util.*

class HyperskillCheckChoiceTaskTest : CheckersTestBase<Unit>() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  override fun createCourse(): Course = course(courseProducer = ::HyperskillCourse) {
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
  } as HyperskillCourse

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
        when (val path = request.getPathWithoutPrams()) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }
    CheckActionListener.reset()
    CheckActionListener.expectedMessage { "<html>Succeed solution</html>" }
    val task = myCourse.allTasks[0]
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }


  fun `test choice task incorrect`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.getPathWithoutPrams()) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Wrong solution" }
    val task = myCourse.allTasks[0]
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }

  fun `test choice task nothing selected `() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { EduCoreBundle.message("choice.task.empty.variant") }
    val task = myCourse.allTasks[1]
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
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