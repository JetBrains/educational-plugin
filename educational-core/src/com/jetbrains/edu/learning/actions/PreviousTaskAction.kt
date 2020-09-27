package com.jetbrains.edu.learning.actions

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.jetbrains.annotations.NonNls

class PreviousTaskAction : TaskNavigationAction(
  EduCoreBundle.lazyMessage("action.navigation.previous.text"),
  EduCoreBundle.lazyMessage("action.navigation.previous.description"),
  AllIcons.Actions.Back
) {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.previousTask(sourceTask)

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.PreviousTask"
  }
}
