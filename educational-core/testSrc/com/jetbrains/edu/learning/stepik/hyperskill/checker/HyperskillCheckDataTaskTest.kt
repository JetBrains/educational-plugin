package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor.Companion.NO_OUTPUT
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator.Companion.CHECK_RESULT_FILE
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.checker.StepikBasedCheckDataTaskTest
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import java.util.*

class HyperskillCheckDataTaskTest : StepikBasedCheckDataTaskTest() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

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

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    logOutFakeHyperskillUser()
    super.tearDown()
  }

  fun `test solved data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/$SUBMISSION_ID" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.reset()
    val course = getCourse()
    checkTask(course.allTasks[0])
  }

  fun `test failed data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/$SUBMISSION_ID" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        }
      )
    }

    CheckActionListener.shouldFail()
    val course = getCourse()
    checkTask(course.allTasks[0])
  }


  fun `test skipped data task`() {
    CheckActionListener.shouldSkip()
    val course = getCourse()
    checkTask(course.allTasks[1])
  }

  // TODO TIME/TIMER RELATED tests (EDU-4845)

}