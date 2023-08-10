package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType
import java.awt.Point

class MockTaskDescriptionView(project: Project) : TaskDescriptionView(project) {
  override var currentTask: Task? = null

  override fun init(toolWindow: ToolWindow) {}
  override fun showTab(tabType: TabType) {}
  override fun updateCheckPanel(task: Task?) {}
  override fun updateTaskSpecificPanel() {}
  override fun updateNavigationPanel(task: Task?) {}
  override fun updateTaskDescription(task: Task?) {}
  override fun updateTaskDescription() {}
  override fun updateAdditionalTaskTabs(task: Task?) {}
  override fun updateTab(tabType: TabType) {}
  override fun showLoadingSubmissionsPanel(platformName: String) {}

  override fun readyToCheck() {}
  override fun checkStarted(task: Task, startSpinner: Boolean) {}
  override fun checkFinished(task: Task, checkResult: CheckResult) {}
  override fun checkTooltipPosition(): RelativePoint = RelativePoint(Point(0, 0))
}
