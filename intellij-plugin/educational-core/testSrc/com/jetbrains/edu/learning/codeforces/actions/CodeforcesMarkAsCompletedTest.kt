package com.jetbrains.edu.learning.codeforces.actions

import com.jetbrains.edu.learning.codeforces.CodeforcesTestCase
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class CodeforcesMarkAsCompletedTest : CodeforcesTestCase() {
  @Test
  fun `test codeforces task is successfully marked as completed`() {
    val taskFileName = "Task.kt"
    val course = courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask {
          taskFile(taskFileName)
        }
      }
    }
    val task = course.findTask("lesson1", "task1")

    configureByTaskFile(1, 1, taskFileName)
    assertEquals(CheckStatus.Unchecked, task.status)
    testAction(CodeforcesMarkAsCompletedAction.ACTION_ID)
    assertEquals(CheckStatus.Solved, task.status)
  }

  @Test
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
    testAction(CodeforcesMarkAsCompletedAction.ACTION_ID, shouldBeEnabled = false)
    assertEquals(CheckStatus.Unchecked, task.status)
  }
}