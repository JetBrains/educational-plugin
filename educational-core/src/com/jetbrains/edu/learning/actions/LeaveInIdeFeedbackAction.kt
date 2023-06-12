package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.uIfeedback.InIdeFeedbackDialog

class LeaveInIdeFeedbackAction : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.leave.feedback.text"),
  EduCoreBundle.lazyMessage("action.leave.feedback.text"),
  EducationalCoreIcons.CommentTask
), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return

    val dialog = InIdeFeedbackDialog(project, task.course, task, false)
    if (dialog.showAndGet()) {
      dialog.showThanksNotification()
    }

    //EduCounterUsageCollector.leaveFeedback()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
//    if (!project.isStudentProject()) return
    val task = project.getCurrentTask() ?: return
    //val course = task.course
    e.presentation.isEnabledAndVisible = true
  }

  companion object {
    const val ACTION_ID: String = "Educational.LeaveInIdeFeedbackAction"
  }
}