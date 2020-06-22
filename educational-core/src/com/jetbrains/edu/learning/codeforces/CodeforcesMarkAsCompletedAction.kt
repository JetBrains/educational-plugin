package com.jetbrains.edu.learning.codeforces

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState.Companion.getEduState
import com.jetbrains.edu.learning.checker.CheckResult.Companion.SOLVED
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.util.*

class CodeforcesMarkAsCompletedAction
  : DumbAwareAction(EduCoreBundle.message("codeforces.label.mark.codeforces.task.as.completed", CodeforcesNames.CODEFORCES_TITLE)) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = getEduState(project)?.task ?: return
    if (task is CodeforcesTask) {
      task.status = CheckStatus.Solved
      task.feedback = CheckFeedback(checkResult = SOLVED, time = Date())
      ProjectView.getInstance(project).refresh()
      TaskDescriptionView.getInstance(project).updateCheckPanel(task)
      showSuccessNotification(project)
    }
    else {
      showWrongTaskNotification(project)
    }
  }

  private fun showSuccessNotification(project: Project) {
    val notification = Notification(notificationGroup.displayId,
                                    "",
                                    EduCoreBundle.message("codeforces.mark.as.completed.notification"),
                                    NotificationType.INFORMATION)
    notification.notify(project)
  }

  private fun showWrongTaskNotification(project: Project) {
    val notification = Notification(
      notificationGroup.displayId,
      "",
      EduCoreBundle.message("codeforces.mark.as.completed.wrong.task.notification", CodeforcesNames.CODEFORCES_TITLE),
      NotificationType.ERROR
    )
    notification.notify(project)
  }

  companion object {
    const val ACTION_ID = "Codeforces.MarkAsCompleted"

    val notificationGroup = NotificationGroup(EduCoreBundle.message("codeforces.information", CodeforcesNames.CODEFORCES_TITLE),
                                              NotificationDisplayType.BALLOON,
                                              true)
  }
}