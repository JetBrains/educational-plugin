package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import org.junit.Test

class SolutionHiddenTest : EduTestCase() {
  @Test
  fun `test do not show solution when it's hidden for course and not specified for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = true, solutionHiddenInTask = null, expectedSolutionHiddenInTask = true)

  @Test
  fun `test show solution when it's visible for course and not specified for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = false, solutionHiddenInTask = null, expectedSolutionHiddenInTask = false)

  @Test
  fun `test do not show solution when it's hidden for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = false, solutionHiddenInTask = true, expectedSolutionHiddenInTask = true)

  @Test
  fun `test show solution when it's visible for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = true, solutionHiddenInTask = false, expectedSolutionHiddenInTask = false)

  private fun doTestSolutionHidden(solutionsHiddenInCourse: Boolean,
                                   solutionHiddenInTask: Boolean?,
                                   expectedSolutionHiddenInTask: Boolean) {
    val course = getCurrentCourse()
    course.solutionsHidden = solutionsHiddenInCourse
    val task = findTask(0, 0)
    task.solutionHidden = solutionHiddenInTask

    assertEquals(expectedSolutionHiddenInTask, !task.canShowSolution())
  }

  @Test
  fun `test do not show solution when answer is empty`() {
    courseWithFiles("Edu test course") {
      lesson(name = "lesson1") {
        eduTask(name = "task1") {
          taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
            placeholder(0, "")
          }
        }
      }
    }
    assertFalse(findTask(0, 0).canShowSolution())
  }

  @Test
  fun `test do not show solution when no answer provided`() {
    courseWithFiles("Edu test course") {
      lesson(name = "lesson1") {
        eduTask(name = "task1") {
          taskFile("taskFile1.txt", "a = <p>TODO()</p>")
        }
      }
    }
    assertFalse(findTask(0, 0).canShowSolution())
  }

  @Test
  fun `test do not show solution when there are no placeholders`() {
    courseWithFiles("Edu test course") {
      lesson(name = "lesson1") {
        eduTask(name = "task1") {
          taskFile("taskFile1.txt")
        }
      }
    }
    assertFalse(findTask(0, 0).canShowSolution())
  }

  @Test
  fun `test show hidden solution if task is solved`() {
    getCurrentCourse()
    val task = findTask(0, 0)
    task.solutionHidden = true
    task.status = CheckStatus.Solved

    assertTrue(task.canShowSolution())
  }

  private fun getCurrentCourse(): Course {
    return courseWithFiles("Edu test course") {
      lesson(name = "lesson1") {
        eduTask(name = "task1") {
          taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
            placeholder(0, "2")
          }
        }
      }
    }
  }
}