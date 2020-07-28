package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class PlainTextCheckerTest : CheckersTestBase<Unit>() {

  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  override fun createCourse(): Course {
    return course {
      lesson {
        outputTask("OutputTask") {
          taskFile(PlainTextConfigurator.CHECK_RESULT_FILE) {
            withText("OK!\n")
          }
          taskFile("output.txt") {
            withText("OK!\n")
          }
        }
        outputTask("OutputTaskWithWindowsLineSeparators") {
          taskFile(PlainTextConfigurator.CHECK_RESULT_FILE) {
            withText("OK!\n")
          }
          taskFile("output.txt") {
            withText("OK!\r\n")
          }
        }
      }
    }
  }

  fun `test course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        is TheoryTask -> ""
        else -> null
      }
    }
    doTest()
  }

}
