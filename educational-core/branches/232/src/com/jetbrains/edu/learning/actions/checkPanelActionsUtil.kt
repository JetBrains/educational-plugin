package com.jetbrains.edu.learning.actions

import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun getLeaveFeedbackActionId(task: Task): String {
  return if (!task.course.isMarketplace) {
    LeaveFeedbackAction.ACTION_ID
  }
  else {
    LeaveInIdeFeedbackAction.ACTION_ID
  }
}