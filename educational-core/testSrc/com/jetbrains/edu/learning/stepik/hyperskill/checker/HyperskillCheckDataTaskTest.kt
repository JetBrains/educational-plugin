package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor.Companion.NO_OUTPUT
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator.Companion.CHECK_RESULT_FILE
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import org.intellij.lang.annotations.Language
import java.util.*

class HyperskillCheckDataTaskTest : HyperskillCheckActionTestBase() {

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("Topic name") {
          dataTask(
            DATA_TASK_1,
            stepId = 1,
            attempt = Attempt(ATTEMPT_ID_OF_SUCCEED_SUBMISSION, Date(0), 300).toDataTaskAttempt()
          ) {
            taskFile(CHECK_RESULT_FILE, "some text")
            dir(DATA_FOLDER_NAME) {
              dir(DATASET_FOLDER_NAME) {
                taskFile(INPUT_FILE_NAME, "some text")
              }
            }
          }
          dataTask(
            DATA_TASK_2,
            stepId = 2,
            attempt = Attempt(ATTEMPT_ID_OF_FAILED_SUBMISSION, Date(0), 300).toDataTaskAttempt()
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
    }
  }

  fun `test solved data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/$SUBMISSION_ID" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    checkCheckAction(getCourse().allTasks[0], CheckStatus.Solved)
  }

  fun `test failed data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/$SUBMISSION_ID" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    checkCheckAction(getCourse().allTasks[0], CheckStatus.Failed)
  }

  fun `test skipped data task`() {
    checkCheckAction(getCourse().allTasks[1], CheckStatus.Unchecked)
  }

  // TODO TIME/TIMER RELATED tests (EDU-4845)

  companion object {
    private val DATA_TASK_1: String = "Data Task 1"
    private val DATA_TASK_2: String = "Data Task 2"

    private val ATTEMPT_ID_OF_SUCCEED_SUBMISSION: Int = 101
    private val ATTEMPT_ID_OF_FAILED_SUBMISSION: Int = 102

    private val SUBMISSION_ID: Int = 100

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
          "attempt": "${ATTEMPT_ID_OF_SUCCEED_SUBMISSION}",
          "id": "${SUBMISSION_ID}",
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
          "attempt": "${ATTEMPT_ID_OF_SUCCEED_SUBMISSION}",
          "id": "${SUBMISSION_ID}",
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
          "attempt": "${ATTEMPT_ID_OF_FAILED_SUBMISSION}",
          "id": "${SUBMISSION_ID}",
          "status": "wrong",
          "step": 2,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 123
        }
      ]
    }
  """
  }
}