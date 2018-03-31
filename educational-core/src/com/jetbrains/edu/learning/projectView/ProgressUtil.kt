package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks

object ProgressUtil {
  /**
   * Counts current progress for course which consists of only on task with subtasks
   * In this case we count each subtasks as task
   * @return Pair (number of solved tasks, number of tasks) or null if lessons can't be interpreted as one task with subtasks
   */
  @JvmStatic
  fun countProgressAsOneTaskWithSubtasks(lessons: List<Lesson>): Pair<Int, Int>? {
    if (lessons.size == 1 && lessons[0].taskListForProgress.size == 1) {
      val lesson = lessons[0]
      val task = lesson.taskListForProgress[0]
      if (task is TaskWithSubtasks) {
        val lastSubtaskIndex = task.lastSubtaskIndex
        val activeSubtaskIndex = task.activeSubtaskIndex
        val taskNum = lastSubtaskIndex + 1
        val isLastSubtaskSolved = activeSubtaskIndex == lastSubtaskIndex && task.getStatus() == CheckStatus.Solved
        return Pair(if (isLastSubtaskSolved) taskNum else activeSubtaskIndex, taskNum)
      }
    }
    return null
  }

  /**
   * @return Pair (number of solved tasks, number of tasks)
   */
  @JvmStatic
  fun countProgressWithoutSubtasks(course: Course): Pair<Int, Int> {
    var taskNum = 0
    var taskSolved = 0
    course.visitLessons({lesson, _ ->
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
