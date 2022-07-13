package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType
import java.awt.Color

abstract class TaskDescriptionView {

  abstract var currentTask: Task?

  abstract fun init(toolWindow: ToolWindow)

  abstract fun showTab(tabType: TabType)
  abstract fun updateCheckPanel(task: Task?)
  abstract fun updateTaskSpecificPanel()
  abstract fun updateTopPanel(task: Task?)
  abstract fun updateTaskDescription(task: Task?)
  abstract fun updateTaskDescription()
  abstract fun updateAdditionalTaskTabs(task: Task? = null)
  abstract fun updateTab(tabType: TabType)
  abstract fun showLoadingSubmissionsPanel(platformName: String)

  abstract fun readyToCheck()
  abstract fun checkStarted(task: Task, startSpinner: Boolean = false)
  abstract fun checkFinished(task: Task, checkResult: CheckResult)
  abstract fun checkTooltipPosition(): RelativePoint?

  companion object {

    @JvmStatic
    fun getInstance(project: Project): TaskDescriptionView {
      if (!EduUtils.isEduProject(project)) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return project.service()
    }

    @JvmStatic
    fun getTaskDescriptionBackgroundColor(): Color {
      return UIUtil.getListBackground()
    }

    @JvmStatic
    fun updateAllTabs(project: Project) {
      getInstance(project).apply {
        updateTaskDescription()
        updateAdditionalTaskTabs()
      }
    }
  }
}
