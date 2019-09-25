package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution

class SolutionHiddenTest : EduTestCase() {
  fun `test is solution hidden when course value == true`() =
    doTestSolutionHidden(solutionsHiddenInCourse = true, solutionHiddenInTask = null, expectedSolutionHiddenInTask = true)

  fun `test is solution hidden when course value == false`() =
    doTestSolutionHidden(solutionsHiddenInCourse = false, solutionHiddenInTask = null, expectedSolutionHiddenInTask = false)

  fun `test is solution hidden when task value == true`() =
    doTestSolutionHidden(solutionsHiddenInCourse = false, solutionHiddenInTask = true, expectedSolutionHiddenInTask = true)

  fun `test is solution hidden when task value == false`() =
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
}