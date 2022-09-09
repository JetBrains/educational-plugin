package com.jetbrains.edu.learning.stepik.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor.Companion.NO_OUTPUT
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator.Companion.CHECK_RESULT_FILE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.stepik.StepikTestUtils.logOutFakeStepikUser
import com.jetbrains.edu.learning.stepik.StepikTestUtils.loginFakeStepikUser
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.http.HttpStatus
import java.util.*

class StepikCheckDataTaskTest : StepikBasedCheckDataTaskTest() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  override fun createCourse(): Course = course {
    section(SECTION) {
      lesson(LESSON) {
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
  }.apply { id = 1 }

  override fun setUp() {
    super.setUp()
    loginFakeStepikUser()
  }

  override fun tearDown() {
    logOutFakeStepikUser()
    super.tearDown()
  }

  fun `test solved data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      MockResponseFactory.fromString(
        when (path) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/$SUBMISSION_ID" -> submissionWithSucceedStatus
          else -> error("Wrong path: ${path}")
        },
        responseCode = HttpStatus.SC_CREATED
      )
    }

    CheckActionListener.reset()
    checkTask(myCourse.allTasks[0])
  }

  fun `test failed data task`() {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      MockResponseFactory.fromString(
        when (path) {
          "/api/submissions" -> submissionWithEvaluationStatus
          "/api/submissions/$SUBMISSION_ID" -> submissionWithFailedStatus
          else -> error("Wrong path: ${path}")
        },
        responseCode = HttpStatus.SC_CREATED
      )
    }

    CheckActionListener.shouldFail()
    checkTask(myCourse.allTasks[0])
  }


  fun `test skipped data task`() {
    CheckActionListener.shouldSkip()
    checkTask(myCourse.allTasks[1])
  }

  // TODO TIME/TIMER RELATED tests (EDU-4845)

  companion object {
    private const val SECTION: String = "Section"
    private const val LESSON: String = "Lesson"
  }
}