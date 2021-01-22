package com.jetbrains.edu.learning.actions.navigate.hyperskill

import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillLegacyProblemsNavigationTest : HyperskillNavigateInCourseTestBase() {
  override val course: HyperskillCourse
    get() = createHyperskillCourse(withLegacyProblems = true)

  override fun getFirstProblem(): Task = course.findTask(HYPERSKILL_PROBLEMS, problem1Name)

  fun `test navigate to next available on first problem`() =
    checkNavigationAction(getFirstProblem(), ::NextTaskAction, true)

  fun `test navigate to next unavailable on last problem`() {
    val secondProblem = course.findTask(HYPERSKILL_PROBLEMS, problem2Name)
    checkNavigationAction(secondProblem, ::NextTaskAction, false)
  }
}