package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import org.jetbrains.annotations.NonNls

class NextTaskAction : TaskNavigationAction() {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.nextTask(sourceTask)

  override fun update(e: AnActionEvent) {
    if (CheckPanel.ACTION_PLACE == e.place) {
      //action is being added only in valid context
      //no project in event in this case, so just enable it
      return
    }
    super.update(e)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.NextTask"
  }
}
