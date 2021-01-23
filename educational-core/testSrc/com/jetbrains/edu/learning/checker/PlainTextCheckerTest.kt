package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
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
          dir("tests") {
            taskFile("output.txt") {
              withText("OK!\n")
            }
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
        eduTask {
          dir("tests") {
            taskFile("Tests.txt") {
              withText("test file text")
            }
          }
          taskFile("task.txt") {
            withText("task file")
          }
          taskFile(PlainTextConfigurator.CHECK_RESULT_FILE) {
            withText("Solved Congratulations!")
          }
        }
      }
    }
  }

  fun `test course`() {
    withFeature(EduExperimentalFeatures.MARKETPLACE, false) {
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

  fun `test test files content created and deleted in output task`() {
    withFeature(EduExperimentalFeatures.MARKETPLACE, true) {
      val allTasks = myCourse.allTasks
      val outputTask = allTasks[0]

      val testFiles = outputTask.taskFiles.values.filter { outputTask.shouldBeEmpty(it.name) }
      assertEquals(1, testFiles.size)
      val taskDir = outputTask.getDir(project.courseDir) ?: error("No task dir found")
      val testFile = testFiles[0]
      val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: error("no virtual file found for the test file")

      assertEmpty(vTestFile.document.text)
      checkTask(outputTask)
      assertEmpty(vTestFile.document.text)
    }
  }

  fun `test test files content created and deleted in edu task`() {
    withFeature(EduExperimentalFeatures.MARKETPLACE, true) {
      val allTasks = myCourse.allTasks
      val eduTask = allTasks[2]

      val testFiles = eduTask.taskFiles.values.filter { eduTask.shouldBeEmpty(it.name) }
      assertEquals(1, testFiles.size)
      val taskDir = eduTask.getDir(project.courseDir) ?: return@withFeature fail()
      val testFile = testFiles[0]
      val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: return@withFeature fail()

      assertEmpty(vTestFile.document.text)
      checkTask(eduTask)
      assertEmpty(vTestFile.document.text)
    }
  }

}
