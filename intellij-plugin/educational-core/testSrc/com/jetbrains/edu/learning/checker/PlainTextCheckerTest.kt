package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.configuration.PlainTextTaskCheckerProvider.Companion.CHECK_RESULT_FILE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class PlainTextCheckerTest : CheckersTestBase<EmptyProjectSettings>() {

  override fun createCheckerFixture(): EduCheckerFixture<EmptyProjectSettings> = PlaintTextCheckerFixture()

  override fun createCourse(): Course {
    return course {
      lesson {
        outputTask("OutputTask") {
          taskFile(CHECK_RESULT_FILE) {
            withText("OK!\n")
          }
          dir("tests") {
            taskFile("output.txt") {
              withText("OK!\n")
            }
          }
        }
        outputTask("OutputTaskWithWindowsLineSeparators") {
          taskFile(CHECK_RESULT_FILE) {
            withText("OK!\n")
          }
          taskFile("output.txt") {
            withText("OK!\r\n")
          }
        }
        eduTask("EduTask") {
          taskFile("task.txt") {
            withText("task file")
          }
          taskFile(CHECK_RESULT_FILE) {
            withText("Solved Congratulations!")
          }
          dir("tests") {
            taskFile("Tests.txt") {
              withText(EDU_TEST_FILE_TEXT)
            }
          }
        }
        eduTask("EduTaskWithVisibleTests") {
          taskFile("task.txt") {
            withText("task file")
          }
          taskFile(CHECK_RESULT_FILE) {
            withText("Solved Congratulations!")
          }
          dir("tests") {
            taskFile("Tests.txt", visible = true) {
              withText(EDU_TEST_FILE_TEXT)
            }
          }
        }
      }
    }.apply { isMarketplace = true }
  }

  @Test
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

  @Test
  fun `test test files content created and deleted in output task`() {
    val outputTask = myCourse.allTasks.single { it.name == "OutputTask" }

    val testFiles = outputTask.taskFiles.values.filter { outputTask.shouldBeEmpty(it.name) }
    assertEquals(1, testFiles.size)
    val taskDir = outputTask.getDir(project.courseDir) ?: error("No task dir found")
    val testFile = testFiles[0]
    val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: error("no virtual file found for the test file")

    assertEmpty(vTestFile.document.text)
    checkTask(outputTask)
    assertEmpty(vTestFile.document.text)
  }

  @Test
  fun `test test files content created and deleted in edu task`() {
    val eduTask = myCourse.allTasks.single { it.name == "EduTask" }

    val testFiles = eduTask.taskFiles.values.filter { eduTask.shouldBeEmpty(it.name) }
    assertEquals(1, testFiles.size)
    val taskDir = eduTask.getDir(project.courseDir) ?: error("No task dir found")
    val testFile = testFiles[0]
    val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: error("no virtual file found for the test file")

    assertEmpty(vTestFile.document.text)
    checkTask(eduTask)
    assertEmpty(vTestFile.document.text)
  }

  @Test
  fun `test visible test files content is not change in edu task`() {
    val eduTask = myCourse.allTasks.first { it.name == "EduTaskWithVisibleTests" }

    val testFiles = eduTask.taskFiles.values.filter { eduTask.shouldBeEmpty(it.name) }
    assertEquals(0, testFiles.size)
    val taskDir = eduTask.getDir(project.courseDir) ?: error("No task dir found")
    val testFile = eduTask.taskFiles.values.single { EduUtilsKt.isTestsFile(eduTask, it.name) }
    val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: error("no virtual file found for the test file")

    assertEquals(EDU_TEST_FILE_TEXT, vTestFile.document.text)
    checkTask(eduTask)
    assertEquals(EDU_TEST_FILE_TEXT, vTestFile.document.text)
  }

  companion object {
    private const val EDU_TEST_FILE_TEXT = "test file text"
  }
}
