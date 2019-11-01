package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution

class SolutionHiddenTest : EduTestCase() {
  fun `test do not show solution when it's hidden for course and not specified for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = true, solutionHiddenInTask = null, expectedSolutionHiddenInTask = true)

  fun `test show solution when it's visible for course and not specified for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = false, solutionHiddenInTask = null, expectedSolutionHiddenInTask = false)

  fun `test do not show solution when it's hidden for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = false, solutionHiddenInTask = true, expectedSolutionHiddenInTask = true)

  fun `test show solution when it's visible for task`() =
    doTestSolutionHidden(solutionsHiddenInCourse = true, solutionHiddenInTask = false, expectedSolutionHiddenInTask = false)

  private fun doTestSolutionHidden(solutionsHiddenInCourse: Boolean,
                                   solutionHiddenInTask: Boolean?,
                                   expectedSolutionHiddenInTask: Boolean) {
    val course = courseWithFiles("Edu test course") {
      lesson(name = "lesson1") {
        eduTask(name = "task1") {
          taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
            placeholder(0, "2")
          }
        }
      }
    }
    course.solutionsHidden = solutionsHiddenInCourse
    val task = findTask(0, 0)
    task.solutionHidden = solutionHiddenInTask

    assertEquals(expectedSolutionHiddenInTask, !task.canShowSolution())
  }

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
}