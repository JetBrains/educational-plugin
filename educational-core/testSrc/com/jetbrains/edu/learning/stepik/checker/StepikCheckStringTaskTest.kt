package com.jetbrains.edu.learning.stepik.checker

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.stepik.StepikTestUtils.logOutFakeStepikUser
import com.jetbrains.edu.learning.stepik.StepikTestUtils.loginFakeStepikUser
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.http.HttpStatus

class StepikCheckStringTaskTest : StepikBasedCheckStringTaskTest() {
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
        stringTask(stepId = 1, name = "1_string_task") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, text = "<p>answer</p>")
        }
        stringTask(stepId = 1, name = "2_string_task") {
          taskFile(AnswerTask.ANSWER_FILE_NAME, "<p></p>")
        }
      }
    }
  }.apply { id = 1 }
}