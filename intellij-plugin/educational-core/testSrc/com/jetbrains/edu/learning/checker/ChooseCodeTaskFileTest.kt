package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class ChooseCodeTaskFileTest : EduTestCase() {
  @Test
  fun `test choose the only task file`() = doTest("task1", "TheOnlyTaskFile.txt")

  @Test
  fun `test choose Main task file specified in configurator`() = doTest("task2", "Main.txt")

  @Test
  fun `test choose Task task file specified in configurator`() = doTest("task5", "Task.txt")

  @Test
  fun `test choose task file opened in editor`() = doTest("task3", "2.txt", "lesson1/task3/2.txt")

  @Test
  fun `test choose Main task file when file from other task is opened in editor`() =
    doTest("task2", "Main.txt", "lesson1/task3/1.txt")

  @Test
  fun `test choose first suitable task file`() = doTest("task4", "3.txt")

  @Test
  fun `test choose user created file when it's best candidate and it's opened in editor`() =
    doTest("task4", "2.txt", "lesson1/task4/2.txt")

  private fun doTest(taskName: String, taskFileName: String, openedFile: String? = null) {
    val task = getTask(taskName)
    if (openedFile != null) {
      myFixture.openFileInEditor(myFixture.findFileInTempDir(openedFile))
    }
    val taskFile = task.getCodeTaskFile(project) ?: error("Unable to find taskFile $taskFileName for task $taskName")
    assertEquals(task.getTaskFile(taskFileName), taskFile)
  }

  private fun getTask(taskName: String): Task {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("TheOnlyTaskFile.txt")
        }
        eduTask {
          taskFile("Foo.kt")
          taskFile("Task.txt")
          taskFile("Main.txt")
        }
        eduTask {
          taskFile("1.txt")
          taskFile("2.txt")
          taskFile("3.txt")
        }
        eduTask {
          taskFile("1.txt", visible = false)
          taskFile("2.txt") {
            taskFile.isLearnerCreated = true
          }
          taskFile("3.txt")
        }
        eduTask {
          taskFile("Foo.kt")
          taskFile("Bar.txt")
          taskFile("Task.txt")
        }
      }
    }
    return course.findTask("lesson1", taskName)
  }
}
