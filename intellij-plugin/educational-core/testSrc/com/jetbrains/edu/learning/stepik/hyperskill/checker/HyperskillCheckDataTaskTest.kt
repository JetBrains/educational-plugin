package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor.Companion.NO_OUTPUT
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.pathWithoutPrams
import org.intellij.lang.annotations.Language
import org.junit.Test
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
            checkResultFile("some text")
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
            checkResultFile(NO_OUTPUT)
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

  @Test
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

  @Test
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

  @Test
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