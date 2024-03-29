package com.jetbrains.edu.learning.actions

import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.jetbrains.annotations.NonNls

class PreviousTaskAction : TaskNavigationAction() {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.previousTask(sourceTask)

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.PreviousTask"
  }
}
