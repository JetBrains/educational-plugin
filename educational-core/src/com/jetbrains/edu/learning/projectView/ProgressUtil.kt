package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

object ProgressUtil {
  /**
   * @return Pair (number of solved tasks, number of tasks)
   */
  @JvmStatic
  fun countProgress(course: Course): Pair<Int, Int> {
    if (course is HyperskillCourse) {
      // we want empty progress in case project stages are not loaded
      // and only code challenges are present
      val projectLesson = course.getProjectLesson() ?: return 0 to 0
      return countProgress(projectLesson)
    }
    var taskNum = 0
    var taskSolved = 0
    course.visitLessons { lesson ->
      taskNum += lesson.taskList.size
      taskSolved += getSolvedTasks(lesson)
    }
    return Pair(taskSolved, taskNum)
  }

  @JvmStatic
  fun countProgress(lesson: Lesson): Pair<Int, Int> {
    val taskNum = lesson.taskList.size
    val taskSolved = getSolvedTasks(lesson)
    return Pair(taskSolved, taskNum)
  }

  private fun getSolvedTasks(lesson: Lesson): Int {
    return lesson.taskList
      .filter { it.status == CheckStatus.Solved }
      .count()
  }
}
