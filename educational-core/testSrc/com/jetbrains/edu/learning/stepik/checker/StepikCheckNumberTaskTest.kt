package com.jetbrains.edu.learning.stepik.checker

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.stepik.StepikTestUtils.logOutFakeStepikUser
import com.jetbrains.edu.learning.stepik.StepikTestUtils.loginFakeStepikUser
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.http.HttpStatus

class StepikCheckNumberTaskTest : StepikBasedCheckNumberTaskTest() {
  override val defaultResponseCode: Int = HttpStatus.SC_CREATED

  override val mockConnector: MockStepikConnector
    get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
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
      }
    }
  }.apply { id = 1 }
}