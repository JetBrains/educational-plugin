package com.jetbrains.edu.learning.actions

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils

class PreviousTaskAction : TaskNavigationAction(
  EduCoreActionBundle.message("action.navigation.previous.text"),
  EduCoreActionBundle.message("action.navigation.previous.description"),
  AllIcons.Actions.Back
) {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.previousTask(sourceTask)

  companion object {
    const val ACTION_ID = "Educational.PreviousTask"
  }
}
