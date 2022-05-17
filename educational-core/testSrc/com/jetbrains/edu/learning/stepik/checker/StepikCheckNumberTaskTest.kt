package com.jetbrains.edu.learning.stepik.checker

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.stepik.StepikTestUtils.logOutFakeStepikUser
import com.jetbrains.edu.learning.stepik.StepikTestUtils.loginFakeStepikUser
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.http.HttpStatus
import org.intellij.lang.annotations.Language

class StepikCheckNumberTaskTest : StepikBasedCheckNumberTaskTest() {
  override val defaultResponseCode: Int = HttpStatus.SC_CREATED

  override val mockConnector: MockStepikConnector
    get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      // Can be called from `StepikStartupActivity` after login
      if (request.path.startsWith("/api/submissions?")) MockResponseFactory.fromString(EMPTY_SUBMISSIONS) else null
    }
    loginFakeStepikUser()
  }

  override fun tearDown() {
    logOutFakeStepikUser()
    super.tearDown()
  }

  override fun createCourse(): Course = course {
    section(SECTION) {
      lesson(LESSON) {
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
        numberTask(stepId = 1, name = "8_number_task_ne_line_at_eof") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>1.2</p>")
          taskFile("taskFile.txt", text = "")
        }
        eduTask(name = "9_edu_task") {
          taskFile("test.txt", text = "text")
        }
      }
    }
  }.apply { id = 1 }

  companion object {
    @Language("JSON")
    private const val EMPTY_SUBMISSIONS = """
      {
        "meta": {
          "page": 1,
          "has_next": false,
          "has_previous": false
        },
        "submissions": []
      }
    """
  }
}