package com.jetbrains.edu.learning.actions.navigate.hyperskill

import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillTopicProblemsNavigationTest : HyperskillNavigateInCourseTestBase() {
  override val course: HyperskillCourse
    get() = createHyperskillCourse(withTopicProblems = true)

  override fun getFirstProblemsTask(): Task = findTopicProblem(topic1LessonName, theoryTaskName)

  fun `test navigate to next available on first problems task`() =
    checkNavigationAction(getFirstProblemsTask(), ::NextTaskAction, true)

  fun `test navigate to next available on last problems task of first topic`() {
    val problem = findTopicProblem(topic1LessonName, problem2Name)
    checkNavigationAction(problem, ::NextTaskAction, true)
  }

  fun `test navigate to previous unavailable on first problems task of second topic`() {
    val problem = findTopicProblem(topic2LessonName, theoryTaskName)
    checkNavigationAction(problem, ::PreviousTaskAction, false)
  }
}