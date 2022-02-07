package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.stepik.checker.StepikBasedCheckNumberTaskTest
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import org.apache.http.HttpStatus

class HyperskillCheckNumberTaskTest : StepikBasedCheckNumberTaskTest() {
  override val defaultResponseCode: Int = HttpStatus.SC_OK

  override val mockConnector: MockHyperskillConnector
    get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    logOutFakeHyperskillUser()
    super.tearDown()
  }

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
}