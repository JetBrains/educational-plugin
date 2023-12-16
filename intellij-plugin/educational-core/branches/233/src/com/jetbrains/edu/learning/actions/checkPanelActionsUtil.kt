package com.jetbrains.edu.learning.actions

import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun Task.getLeaveFeedbackActionId(): String {
  return if (!course.isMarketplace) {
    LeaveFeedbackAction.ACTION_ID
  }
  else {
    LeaveInIdeFeedbackAction.ACTION_ID
  }
}

fun isLeaveFeedbackActionAvailable(task: Task): Boolean = true