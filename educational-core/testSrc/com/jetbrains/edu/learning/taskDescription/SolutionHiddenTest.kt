package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class SolutionHiddenTest : EduTestCase() {
  private fun Task.solutionIsHidden() = !canShowSolution()

  fun `test is solution hidden when task is not set`() {
    val task = createTask()

    assertTrue(task.apply(true).solutionIsHidden())
    assertFalse(task.apply(false).solutionIsHidden())
  }

  fun `test is solution hidden when task is set`() {
    val task = createTask()

    assertFalse(task.apply(solutionsHiddenInCourse = true, solutionHiddenInTask = false).solutionIsHidden())
    assertTrue(task.apply(solutionsHiddenInCourse = false, solutionHiddenInTask = true).solutionIsHidden())
  }

  private fun createTask(): Task {
    val course = courseWithFiles("Edu test course") {
      lesson(name = "lesson1") {
        eduTask(name = "task1") {
          taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
            placeholder(0, "2")
          }
        }
      }
    }
    return course.findTask("lesson1", "task1")
  }

  private fun Task.apply(solutionsHiddenInCourse: Boolean, solutionHiddenInTask: Boolean? = null): Task {
    course.solutionsHidden = solutionsHiddenInCourse
    solutionHidden = solutionHiddenInTask
    return this
  }
}