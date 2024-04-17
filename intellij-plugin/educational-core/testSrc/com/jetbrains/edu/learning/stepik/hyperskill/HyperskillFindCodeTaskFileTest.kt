package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleHyperskillConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class HyperskillFindCodeTaskFileTest : EduTestCase() {
  @Test
  fun `test find the only file`() {
    val answerFileName = "src/Task.kt"
    val taskName = "task1"
    val course = hyperskillCourse {
      lesson(LESSON_NAME) {
        codeTask(taskName) {
          taskFile(answerFileName)
        }
      }
    }
    val task = course.findTask(LESSON_NAME, taskName) as CodeTask
    val answerFile = task.taskFiles[answerFileName]

    doTest(task, answerFile)
  }

  @Test
  fun `test find Main file`() {
    val answerFileName = "src/Main.kt"
    val taskName = "task1"
    val course = hyperskillCourse {
      lesson(LESSON_NAME) {
        codeTask(taskName) {
          taskFile("src/Task.kt")
          taskFile(answerFileName)
        }
      }
    }
    val task = course.findTask(LESSON_NAME, taskName) as CodeTask
    val answerFile = task.taskFiles[answerFileName]

    doTest(task, answerFile)
  }

  @Test
  fun `test find first visible & educator-created file`() {
    val answerFileName = "src/Task.kt"
    val taskName = "task1"
    val course = hyperskillCourse {
      lesson(LESSON_NAME) {
        codeTask(taskName) {
          taskFile("src/Foo.kt", visible = true) {
            taskFile.isLearnerCreated = true
          }
          taskFile(answerFileName)
          taskFile("src/Bar.kt", visible = false)
          taskFile("src/Baz.kt")
        }
      }
    }
    val task = course.findTask(LESSON_NAME, taskName) as CodeTask
    val answerFile = task.taskFiles[answerFileName]

    doTest(task, answerFile)
  }

  @Test
  fun `test unable to find file`() {
    val taskName = "task1"
    val course = hyperskillCourse {
      lesson(LESSON_NAME) {
        codeTask(taskName) {
          taskFile("src/Foo.kt", visible = false)
          taskFile("src/Bar.kt") {
            taskFile.isLearnerCreated = true
          }
        }
      }
    }
    val task = course.findTask(LESSON_NAME, taskName) as CodeTask
    doTest(task, null)
  }

  private fun doTest(task: CodeTask, answerFile: TaskFile?) {
    val configurator = FakeGradleHyperskillConfigurator()
    val codeTaskFile = configurator.getCodeTaskFile(project, task)
    assertEquals(answerFile, codeTaskFile)
  }

  companion object {
    private const val LESSON_NAME = "lesson"
  }
}
