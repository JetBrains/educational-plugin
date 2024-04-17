package com.jetbrains.edu.learning.actions.navigate.hyperskill

import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class HyperskillMixedProblemsNavigationTest : HyperskillNavigateInCourseTestBase() {
  override val course: HyperskillCourse
    get() = createHyperskillCourse(withLegacyProblems = true, withTopicProblems = true)

  override fun getFirstProblemsTask(): Task = course.findTask(HYPERSKILL_PROBLEMS, problem1Name)

  @Test
  fun `test navigate to next available on last legacy problem`() {
    val problem = course.findTask(HYPERSKILL_PROBLEMS, problem2Name)
    checkNavigationAction(problem, NextTaskAction.ACTION_ID, true)
  }

  @Test
  fun `test navigate to previous unavailable on first topic problems task`() {
    val problem = findTopicProblem(topic1LessonName, theoryTaskName)
    checkNavigationAction(problem, PreviousTaskAction.ACTION_ID, false)
  }
}