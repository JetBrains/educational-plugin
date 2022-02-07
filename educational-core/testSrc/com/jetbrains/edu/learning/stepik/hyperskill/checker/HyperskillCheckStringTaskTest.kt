package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.stepik.checker.StepikBasedCheckStringTaskTest
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser
import org.apache.http.HttpStatus

class HyperskillCheckStringTaskTest : StepikBasedCheckStringTaskTest() {
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
        stringTask(stepId = 1, name = "1_string_task") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>answer</p>")
        }
        stringTask(stepId = 1, name = "2_string_task") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, "<p></p>")
        }
      }
    }
  }
}