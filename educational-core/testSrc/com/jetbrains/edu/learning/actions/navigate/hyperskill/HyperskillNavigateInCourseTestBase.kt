package com.jetbrains.edu.learning.actions.navigate.hyperskill

import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.actions.TaskNavigationAction
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourseWithFiles

abstract class HyperskillNavigateInCourseTestBase : NavigationTestBase() {
  private val stage1Name = "stage1"
  private val stagesLessonName = "lesson"
  protected val problem1Name = "problem1"
  protected val problem2Name = "problem2"
  protected val topic1LessonName = "topic1"
  protected val topic2LessonName = "topic2"

  abstract val course: HyperskillCourse

  abstract fun getFirstProblem(): Task

  fun `test navigate to next unavailable on last stage`() {
    val task = course.findTask(stagesLessonName, stage1Name)
    return checkNavigationAction(task, ::NextTaskAction, false)
  }

  fun `test navigate to previous unavailable on first problem`() =
    checkNavigationAction(getFirstProblem(), ::PreviousTaskAction, false)

  protected fun findTopicProblem(lessonName: String, problemName: String): Task {
    return course.getTopicsSection()?.getLesson(lessonName)?.getTask(problemName)
           ?: error("Can't find task from topics section")
  }

  protected fun createHyperskillCourse(withLegacyProblems: Boolean = false, withTopicProblems: Boolean = false) =
    hyperskillCourseWithFiles(completeStages = true) {
      frameworkLesson(stagesLessonName) {
        eduTask(stage1Name) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
      if (withLegacyProblems) {
        lesson(HYPERSKILL_PROBLEMS) {
          eduTask(problem1Name) {
            taskFile("src/Task.kt", "fun foo() {}")
          }
          eduTask(problem2Name) {
            taskFile("src/Task.kt", "fun foo() {}")
          }
        }
      }
      if (withTopicProblems) {
        section(HYPERSKILL_TOPICS) {
          lesson(topic1LessonName) {
            eduTask(problem1Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
            eduTask(problem2Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
          }
          lesson(topic2LessonName) {
            eduTask(problem1Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
            eduTask(problem2Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
          }
        }
      }
    }

  protected fun checkNavigationAction(task: Task, action: () -> TaskNavigationAction, expectedStatus: Boolean) {
    val firstTask = task.lesson.taskList.first()
    NavigationUtils.navigateToTask(project, task, firstTask, false)
    task.openTaskFileInEditor("src/Task.kt")
    val presentation = myFixture.testAction(action())
    assertEquals(expectedStatus, presentation.isEnabledAndVisible)
  }
}