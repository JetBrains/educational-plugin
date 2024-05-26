package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.SOLVED
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduInformationNotification
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls
import java.util.*

class CodeforcesMarkAsCompletedAction : CodeforcesAction() {

 override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val task = project.getCurrentTask() as? CodeforcesTask ?: return
    task.status = CheckStatus.Solved
    task.feedback = CheckFeedback(checkResult = SOLVED, time = Date())

    ProjectView.getInstance(project).refresh()
    TaskToolWindowView.getInstance(project).updateCheckPanel(task)
    YamlFormatSynchronizer.saveItem(task)
    showSuccessNotification(project)
  }

  // TODO inline?
  private fun showSuccessNotification(project: Project) {
    val notification = EduInformationNotification(
      content = EduCoreBundle.message("codeforces.mark.as.completed.notification")
    )
    notification.notify(project)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Codeforces.MarkAsCompleted"
  }
}
