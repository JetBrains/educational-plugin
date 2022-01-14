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
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.Step
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillDownloadDatasetTest.Companion.format
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.loginFakeUser
import com.jetbrains.edu.learning.testAction
import org.intellij.lang.annotations.Language
import java.util.*

class HyperskillNumberTaskTest : CheckersTestBase<Unit>() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  override fun createCourse(): Course = course(courseProducer = ::HyperskillCourse) {
    section("Topics") {
      lesson("1_lesson_correct") {
        numberTask(stepId = 1, name = "0_number_task_non_number") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>answer</p>")
        }
        numberTask(stepId = 1, name = "1_number_task_empty") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, "<p></p>")
        }
        numberTask(stepId = 1, name = "2_number_task_correct") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>12</p>")
        }
        numberTask(stepId = 1, name = "3_number_task_with_space") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>12   </p>")
        }
        numberTask(stepId = 1, name = "4_number_task_with_comma") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>\n12   </p>")
        }
        numberTask(stepId = 1, name = "5_number_task_with_comma") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>  12   </p>")
        }
        numberTask(stepId = 1, name = "6_number_task_with_space") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>1,2</p>")
        }
        numberTask(stepId = 1, name = "7_number_task_with_comma") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>1.2</p>")
        }
      }
    }
  }

  override fun setUp() {
    super.setUp()
    loginFakeUser()
  }

  fun `test number task correct`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }
    CheckActionListener.reset()
    CheckActionListener.expectedMessage { "<html>Succeed solution</html>" }
    val task = myCourse.allTasks[2] as NumberTask
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction.ACTION_ID)
    assertEquals("12", task.getInputAnswer(project))
  }


  fun `test number task validation on number`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { EduCoreBundle.message("hyperskill.number.task.not.number") }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[0])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test number task incorrect`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Wrong solution" }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[2])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test number task input is empty`() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { EduCoreBundle.message("hyperskill.string.task.empty.text") }
    NavigationUtils.navigateToTask(project, myCourse.allTasks[1])
    testAction(CheckAction.ACTION_ID)
  }

  fun `test creating placeholder`() {
    val task = myCourse.allTasks[0] as NumberTask
    val lesson = task.lesson
    val stepSource = HyperskillStepSource().apply {
      block = Step().apply {
        name = "number"
      }
    }

    val createdTask = StepikTaskBuilder(myCourse, lesson, stepSource).createTask(stepSource.block?.name!!) ?: error("")
    assertEquals(1, createdTask.taskFiles.size)
    assertEquals(1, createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.size)
    assertEquals(EduCoreBundle.message("string.task.comment.file"),
                 createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.first()?.placeholderText)
    assertEquals(0, createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.first()?.offset)
    assertEquals(EduCoreBundle.message("string.task.comment.file").length,
                 createdTask.getTaskFile(AnswerTask.ANSWER_FILE_NAME)?.answerPlaceholders?.first()?.endOffset)
  }

  fun `test task with space`() {
    assertNull((myCourse.allTasks[3] as NumberTask).validateAnswer (project))
    assertNull((myCourse.allTasks[4] as NumberTask).validateAnswer (project))
    assertNull((myCourse.allTasks[5] as NumberTask).validateAnswer (project))
  }

  fun `test task with comma and dot`() {
    assertNull((myCourse.allTasks[6] as NumberTask).validateAnswer (project))
    assertNull((myCourse.allTasks[7] as NumberTask).validateAnswer (project))
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
        "dataset": { },
        "id": 11,
        "status": "active",
        "step": 1,
        "time": "${Date().format()}",
        "user": 1,
        "time_left": null
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