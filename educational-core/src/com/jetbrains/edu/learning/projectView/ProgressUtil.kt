package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson

object ProgressUtil {
  /**
   * @return Pair (number of solved tasks, number of tasks)
   */
  @JvmStatic
  fun countProgress(course: Course): Pair<Int, Int> {
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
