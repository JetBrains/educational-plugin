package com.jetbrains.edu.learning.actions

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun Task.getLeaveFeedbackActionId(): String {
  return LeaveFeedbackAction.ACTION_ID
}

fun isLeaveStudentFeedbackActionAvailable(task: Task): Boolean {
  if (task.course is HyperskillCourse) return true
  return task.feedbackLink != null
}
