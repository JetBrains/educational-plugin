package com.jetbrains.edu.learning.actions

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils

class NextTaskAction : TaskNavigationAction("Next Task", "Navigate to the next task", AllIcons.Actions.Forward) {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.nextTask(sourceTask)
  override fun getActionId(): String = ACTION_ID
  override fun getShortcuts(): Array<String>? = arrayOf(SHORTCUT)

  companion object {
    const val ACTION_ID = "Educational.NextTask"
    const val SHORTCUT = "ctrl pressed PERIOD"
  }
}
