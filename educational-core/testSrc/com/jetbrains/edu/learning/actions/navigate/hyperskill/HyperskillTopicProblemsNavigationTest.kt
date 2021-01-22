package com.jetbrains.edu.learning.actions.navigate.hyperskill

import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillTopicProblemsNavigationTest : HyperskillNavigateInCourseTestBase() {
  override val course: HyperskillCourse
    get() = createHyperskillCourse(withTopicProblems = true)

  override fun getFirstProblem(): Task = findTopicProblem(topic1LessonName, problem1Name)

  fun `test navigate to next available on first problem`() =
    checkNavigationAction(getFirstProblem(), ::NextTaskAction, true)

  fun `test navigate to next unavailable on last problem of first topic`() {
    val problem = findTopicProblem(topic1LessonName, problem2Name)
    checkNavigationAction(problem, ::NextTaskAction, false)
  }

  fun `test navigate to previous unavailable on first problem of second topic`() {
    val problem = findTopicProblem(topic2LessonName, problem1Name)
    checkNavigationAction(problem, ::PreviousTaskAction, false)
  }
}