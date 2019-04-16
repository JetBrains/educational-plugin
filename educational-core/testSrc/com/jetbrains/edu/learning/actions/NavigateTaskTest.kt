package com.jetbrains.edu.learning.actions

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import junit.framework.TestCase

class NavigateTaskTest : EduTestCase() {

  fun `test next task`() = doNextTest(
    initialTaskFile = TaskFileInfo(1, 2, "taskFile2.txt"),
    expectedTaskFile = TaskFileInfo(1, 3, "taskFile3.txt")
  )

  fun `test previous task`() = doPrevTest(
    initialTaskFile = TaskFileInfo(1, 2, "taskFile2.txt"),
    expectedTaskFile = TaskFileInfo(1, 1, "taskFile1.txt")
  )

  fun `test next lesson`() = doNextTest(
    initialTaskFile = TaskFileInfo(1, 3, "taskFile3.txt"),
    expectedTaskFile = TaskFileInfo(2, 1, "taskFile1.txt")
  )

  fun `test previous lesson`() = doPrevTest(
    initialTaskFile = TaskFileInfo(2, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(1, 3, "taskFile3.txt")
  )

  fun `test last task`() = doNextTest(
    initialTaskFile = TaskFileInfo(2, 4, "taskFile5.txt"),
    expectedTaskFile = TaskFileInfo(2, 4, "taskFile5.txt")
  )

  fun `test first task`() = doPrevTest(
    initialTaskFile = TaskFileInfo(1, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(1, 1, "taskFile1.txt")
  )

  fun `test next task with placeholder`() = doNextTest(
    initialTaskFile = TaskFileInfo(1, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(1, 2, "taskFile2.txt")
  )

  fun `test previous task with placeholder`() = doPrevTest(
    initialTaskFile = TaskFileInfo(1, 3, "taskFile3.txt"),
    expectedTaskFile = TaskFileInfo(1, 2, "taskFile2.txt")
  )

  fun `test do not open invisible task file`() = doNextTest(
    initialTaskFile = TaskFileInfo(2, 1, "taskFile1.txt"),
    expectedTaskFile = TaskFileInfo(2, 2, "taskFile3.txt")
  )

  fun `test open the first task file`() = doNextTest(
    initialTaskFile = TaskFileInfo(2, 3, "taskFile4.txt"),
    expectedTaskFile = TaskFileInfo(2, 4, "taskFile5.txt"))

  private fun doNextTest(initialTaskFile: TaskFileInfo, expectedTaskFile: TaskFileInfo) =
    doTest(NextTaskAction(), initialTaskFile, expectedTaskFile)

  private fun doPrevTest(initialTaskFile: TaskFileInfo, expectedTaskFile: TaskFileInfo) =
    doTest(PreviousTaskAction(), initialTaskFile, expectedTaskFile)

  private fun doTest(action: TaskNavigationAction, initialTaskFile: TaskFileInfo, expectedTaskFile: TaskFileInfo) {
    val (lesson, task, taskFileName) = initialTaskFile
    configureByTaskFile(lesson, task, taskFileName)
    myFixture.testAction(action)
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile ?: error("Can't find current file")
    val taskFile = EduUtils.getTaskFile(myFixture.project, currentFile) ?: error("Can't find current task file")
    val actualTaskFileInfo = TaskFileInfo(lesson = taskFile.task.lesson.index, task = taskFile.task.index, name = taskFile.name)
    TestCase.assertEquals(expectedTaskFile, actualTaskFileInfo)
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
