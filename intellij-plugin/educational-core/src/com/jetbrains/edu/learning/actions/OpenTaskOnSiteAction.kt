package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask.Companion.codeforcesTaskLink
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillTaskLink
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls


class OpenTaskOnSiteAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.open.on.site.text")), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    val link = when {
      task is CodeforcesTask -> codeforcesTaskLink(task)
      task.course is HyperskillCourse -> hyperskillTaskLink(task)
      else -> return
    }
    EduBrowser.getInstance().browse(link)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return
    val task = project.getCurrentTask() ?: return
    val course = task.course

    e.presentation.isEnabledAndVisible = course is CodeforcesCourse || course is HyperskillCourse
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.OpenTaskOnSiteAction"
  }
}