package com.jetbrains.edu.learning.command.validation

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import kotlinx.coroutines.runBlocking

class CourseValidationTest : EduTestCase() {

  override fun runInDispatchThread(): Boolean = false

  override fun tearDown() {
    @Suppress("UnstableApiUsage")
    invokeAndWaitIfNeeded {
      super.tearDown()
    }
  }

  fun `test events for check action`() {
    val course = courseWithFiles {
      lesson("lesson with unchecked tasks") {
        theoryTask("theory unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
        eduTask("edu unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
      }
      lesson("lesson with solved tasks") {
        theoryTask("theory solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        eduTask("edu solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        outputTask("output solved task") {
          checkResultFile("Answer")
          dir("tests") {
            taskFile("output.txt") {
              withText("Answer")
            }
          }
        }
      }
      section("section with failed tasks") {
        lesson("lesson with failed tasks") {
          theoryTask("theory failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          eduTask("edu failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          outputTask("output failed task") {
            checkResultFile("Answer1")
            dir("tests") {
              taskFile("output.txt") {
                withText("Answer2")
              }
            }
          }
        }
      }
    }

    val testMessageConsumer = TestServiceMessageConsumer()
    val validationHelper = CourseValidationHelper(testMessageConsumer)

    runBlocking {
      validationHelper.validate(project, course)
    }

    testMessageConsumer.assertTestTreeEquals("""
      - Test Course:
        - lesson with unchecked tasks:
          - theory unchecked task: ignored
          - edu unchecked task: ignored
        - lesson with solved tasks:
          - theory solved task: success
          - edu solved task: success
          - output solved task: success
        - section with failed tasks:
          - lesson with failed tasks:
            - theory failed task: failed
            - edu failed task: failed
            - output failed task: failed
    """)

  }
}