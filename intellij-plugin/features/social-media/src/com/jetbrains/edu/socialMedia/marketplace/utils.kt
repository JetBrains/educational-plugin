package com.jetbrains.edu.socialMedia.marketplace

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage
import com.jetbrains.edu.learning.projectView.ProgressUtil

const val NUMBER_OF_GIFS = 2
const val SOLVED_TASK_THRESHOLD = 0.8

fun askToPost(solvedTask: Task): Boolean {
  val course = solvedTask.course as? EduCourse ?: return false
  // Show dialog only for Marketplace courses
  if (!course.isMarketplace || course.isFromCourseStorage()) return false

  val (solved, total) = ProgressUtil.countProgress(course)
  return solved > total * SOLVED_TASK_THRESHOLD
}
