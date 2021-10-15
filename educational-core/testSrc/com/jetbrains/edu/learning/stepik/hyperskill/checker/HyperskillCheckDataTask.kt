package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor.Companion.NO_OUTPUT
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.checker.PlaintTextCheckerFixture
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator.Companion.CHECK_RESULT_FILE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.loginFakeUser
import org.intellij.lang.annotations.Language
import java.util.*

class HyperskillCheckDataTask : CheckersTestBase<Unit>() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  override fun createCourse(): Course = course(courseProducer = ::HyperskillCourse) {
    section("Topics") {
      lesson("Topic name") {
        dataTask("Data task 1",
                 attempt = Attempt(101, Date(0), 300).toDataTaskAttempt(),
                 stepId = 1
        ) {
          taskFile(CHECK_RESULT_FILE, "some text")
          dir(DATA_FOLDER_NAME) {
            dir(DATASET_FOLDER_NAME) {
              taskFile(INPUT_FILE_NAME, "some text")
            }
          }
        }
        dataTask("Data task 2",
                 attempt = Attempt(102, Date(0), 300).toDataTaskAttempt(),
                 stepId = 2
        ) {
          // mimics no output result
          taskFile(CHECK_RESULT_FILE, NO_OUTPUT)
          dir(DATA_FOLDER_NAME) {
            dir(DATASET_FOLDER_NAME) {
              taskFile(INPUT_FILE_NAME, "some text")
            }
          }
        }
      }
    }
  } as HyperskillCourse

  override fun setUp() {
    super.setUp()
    loginFakeUser()
  }

  override fun checkTask(task: Task): List<AssertionError> {
    val assertions = super.checkTask(task)
    assertEmpty(assertions)
    return assertions
  }

  fun `test solved data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/100" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.reset()
    checkTask(myCourse.allTasks[0])
  }

  fun `test failed data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/100" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.shouldFail()
    checkTask(myCourse.allTasks[0])
  }


  fun `test skipped data task`() {
    CheckActionListener.shouldSkip()
    checkTask(myCourse.allTasks[1])
  }

  // TODO TIME/TIMER RELATED tests

  @Language("JSON")
  private val submissionWithEvaluationStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "101",
          "id": "100",
          "status": "evaluation",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 123
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
          "attempt": "101",
          "id": "100",
          "status": "succeed",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 123
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
          "attempt": "101",
          "id": "100",
          "status": "wrong",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 123
        }
      ]
    }
  """
}