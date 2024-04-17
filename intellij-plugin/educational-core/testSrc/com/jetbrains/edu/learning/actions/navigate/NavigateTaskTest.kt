package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class NavigateTaskTest : EduTestCase() {

  @Test
  fun `test next task`() = doNextTest(
    initialTaskFile = TaskFileInfo(1, 2, "taskFile2.txt"),
    expectedTaskFile = TaskFileInfo(1, 3, "taskFile3.txt")
  )

  @Test
  fun `test previous task`() = doPrevTest(
    initialTaskFile = TaskFileInfo(1, 2, "taskFile2.txt"),
    expectedTaskFile = TaskFileInfo(1, 1, "taskFile1.txt")
  )

  @Test
  fun `test next lesson`() = doNextTest(
    initialTaskFile = TaskFileInfo(1, 3, "taskFile3.txt"),
    expectedTaskFile = TaskFileInfo(2, 1, "taskFile1.txt")
  )

  @Test
  fun `test previous lesson`() = doPrevTest(
    initialTaskFile = TaskFileInfo(2, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(1, 3, "taskFile3.txt")
  )

  @Test
  fun `test last task`() = doNextTest(
    initialTaskFile = TaskFileInfo(2, 4, "taskFile5.txt"),
    expectedTaskFile = TaskFileInfo(2, 4, "taskFile5.txt")
  )

  @Test
  fun `test first task`() = doPrevTest(
    initialTaskFile = TaskFileInfo(1, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(1, 1, "taskFile1.txt")
  )

  @Test
  fun `test next task with placeholder`() = doNextTest(
    initialTaskFile = TaskFileInfo(1, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(1, 2, "taskFile2.txt")
  )

  @Test
  fun `test previous task with placeholder`() = doPrevTest(
    initialTaskFile = TaskFileInfo(1, 3, "taskFile3.txt"),
    expectedTaskFile = TaskFileInfo(1, 2, "taskFile2.txt")
  )

  @Test
  fun `test do not open invisible task file`() = doNextTest(
    initialTaskFile = TaskFileInfo(2, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(2, 2, "taskFile3.txt")
  )

  @Test
  fun `test open the first task file`() = doNextTest(
    initialTaskFile = TaskFileInfo(2, 3, "taskFile4.txt"),
    expectedTaskFile = TaskFileInfo(2, 4, "taskFile5.txt"))

  private fun doNextTest(initialTaskFile: TaskFileInfo, expectedTaskFile: TaskFileInfo) =
    doTest(NextTaskAction.ACTION_ID, initialTaskFile, expectedTaskFile)

  private fun doPrevTest(initialTaskFile: TaskFileInfo, expectedTaskFile: TaskFileInfo) =
    doTest(PreviousTaskAction.ACTION_ID, initialTaskFile, expectedTaskFile)

  private fun doTest(actionId: String, initialTaskFile: TaskFileInfo, expectedTaskFile: TaskFileInfo) {
    val (lesson, task, taskFileName) = initialTaskFile
    configureByTaskFile(lesson, task, taskFileName)
    testAction(actionId, shouldBeEnabled = initialTaskFile != expectedTaskFile, shouldBeVisible = true)
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile ?: error("Can't find current file")
    val taskFile = currentFile.getTaskFile(myFixture.project) ?: error("Can't find current task file")
    val actualTaskFileInfo = TaskFileInfo(lesson = taskFile.task.lesson.index, task = taskFile.task.index, name = taskFile.name)
    assertEquals(expectedTaskFile, actualTaskFileInfo)
  }

  override fun createCourse() {
    val textWithPlaceholder = "a = <p>TODO()</p>"
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt", textWithPlaceholder) {
            placeholder(0, "hello")
          }
        }
        eduTask {
          taskFile("taskFile3.txt")
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt", visible = false)
          taskFile("taskFile3.txt")
        }
        eduTask {
          taskFile("taskFile4.txt")
        }
        eduTask {
          taskFile("taskFile5.txt", textWithPlaceholder)
          taskFile("taskFile6.txt", textWithPlaceholder)
          taskFile("taskFile7.txt")
        }
      }
    }
  }

  private data class TaskFileInfo(val lesson: Int, val task: Int, val name: String)
}
