package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckDetailsPanel
import java.awt.Color

abstract class TaskDescriptionView {

  abstract var currentTask: Task?

  abstract fun init(toolWindow: ToolWindow)

  abstract fun updateTaskSpecificPanel()
  abstract fun updateTopPanel(task: Task?)
  abstract fun updateTaskDescription(task: Task?)
  abstract fun updateTaskDescription()
  abstract fun updateAdditionalTaskTab()

  abstract fun readyToCheck()
  abstract fun checkStarted()
  abstract fun checkFinished(task: Task, checkResult: CheckResult)
  abstract fun checkTooltipPosition(): RelativePoint

  companion object {

    @JvmStatic
    fun getInstance(project: Project): TaskDescriptionView {
      if (!EduUtils.isEduProject(project)) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return ServiceManager.getService(project, TaskDescriptionView::class.java)
    }

    @JvmStatic
    fun getTaskDescriptionBackgroundColor(): Color {
      return UIUtil.getListBackground()
    }

    @JvmStatic
    fun updateAllTabs(project: Project, taskDescription: TaskDescriptionView) {
      val contentManager = ToolWindowManager.getInstance(project).getToolWindow(
        TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW).contentManager
      val content = contentManager.selectedContent ?: return
      //index is needed to keep current tab opened. On updating tabs we are creating new content,
      // that's why we can't remember current content and set it to contentManager
      val index = contentManager.getIndexOfContent(content)
      taskDescription.updateTaskDescription()
      taskDescription.updateAdditionalTaskTab()
      CheckDetailsPanel.selectTab(project, index)
    }
  }
}
