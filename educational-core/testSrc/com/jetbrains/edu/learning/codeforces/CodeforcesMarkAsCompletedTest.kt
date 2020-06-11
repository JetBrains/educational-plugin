package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.courseFormat.CheckStatus

class CodeforcesMarkAsCompletedTest : CodeforcesTestCase() {
  fun `test codeforces task is successfully marked as completed`() {
    val taskFileName = "Task.kt"
    val course = courseWithFiles {
      lesson {
        codeforcesTask {
          taskFile(taskFileName)
        }
      }
    }
    val task = course.findTask("lesson1", "task1")

    configureByTaskFile(1, 1, taskFileName)
    assertEquals(CheckStatus.Unchecked, task.status)
    myFixture.testAction(CodeforcesMarkAsCompletedAction())
    assertEquals(CheckStatus.Solved, task.status)
  }

  fun `test another task isn't marked as completed`() {
    val taskFileName = "Task.kt"
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile(taskFileName)
        }
      }
    }
    val task = course.findTask("lesson1", "task1")

    configureByTaskFile(1, 1, taskFileName)
    assertEquals(CheckStatus.Unchecked, task.status)
    myFixture.testAction(CodeforcesMarkAsCompletedAction())
    assertEquals(CheckStatus.Unchecked, task.status)
  }
}