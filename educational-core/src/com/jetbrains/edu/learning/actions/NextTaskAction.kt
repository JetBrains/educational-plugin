package com.jetbrains.edu.learning.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckPanel

class NextTaskAction : TaskNavigationAction(
  EduCoreBundle.message("action.navigation.next.text"),
  EduCoreBundle.message("action.navigation.next.description"),
  AllIcons.Actions.Forward
) {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.nextTask(sourceTask)

  override fun update(e: AnActionEvent) {
    if (CheckPanel.ACTION_PLACE == e.place) {
      //action is being added only in valid context
      //no project in event in this case, so just enable it
      return
    }
    e.presentation.text = EduCoreBundle.message("action.navigation.next.text")
    super.update(e)
  }

  companion object {
    const val ACTION_ID = "Educational.NextTask"
  }
}
