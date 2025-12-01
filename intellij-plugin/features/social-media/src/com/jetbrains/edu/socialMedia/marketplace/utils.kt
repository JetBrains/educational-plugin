package com.jetbrains.edu.socialMedia.marketplace

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage

const val NUMBER_OF_GIFS = 2
const val SOLVED_TASK_THRESHOLD = 0.8

fun askToPost(solvedTask: Task): Boolean {
  val course = solvedTask.course as? EduCourse ?: return false
  // Show dialog only for Marketplace courses
  if (!course.isMarketplace || course.isFromCourseStorage()) return false

  var total = 0
  var solved = 0
  course.visitTasks {
    total++
    if (it.status == CheckStatus.Solved) {
      solved++
    }
  }
  return total > 0 && solved >= total * SOLVED_TASK_THRESHOLD
}
