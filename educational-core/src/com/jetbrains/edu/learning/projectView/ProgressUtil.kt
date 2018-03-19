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
    course.visitLessons({lesson ->
      taskNum += lesson.taskListForProgress.size
      taskSolved += getSolvedTasks(lesson)
      true
    })
    return Pair(taskSolved, taskNum)
  }

  private fun getSolvedTasks(lesson: Lesson): Int {
    return lesson.taskListForProgress
      .filter { it.status == CheckStatus.Solved }
      .count()
  }
}
