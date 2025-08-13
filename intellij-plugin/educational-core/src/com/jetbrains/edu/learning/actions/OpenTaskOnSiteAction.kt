package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls


class OpenTaskOnSiteAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.open.on.site.text")), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    val link = EduActionUtils.getOpenOnSiteActionInfo(project, task) ?: return
    EduBrowser.getInstance().browse(link)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = EduActionUtils.getOpenOnSiteActionInfo(project) != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.OpenTaskOnSiteAction"
  }
}