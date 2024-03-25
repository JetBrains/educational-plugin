package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import java.awt.Point

class MockTaskToolWindowView(project: Project) : TaskToolWindowView(project) {
  override var currentTask: Task? = null

  override fun init(toolWindow: ToolWindow) {}
  override fun selectTab(tabType: TabType) {}
  override fun updateCheckPanel(task: Task?) {}
  override fun updateTaskSpecificPanel() {}
  override fun updateNavigationPanel(task: Task?) {}
  override fun updateNavigationPanel() {}
  override fun updateTaskDescription(task: Task?) {}
  override fun updateTaskDescription() {}
  override fun updateTabs(task: Task?) {}
  override fun updateTab(tabType: TabType) {}
  override fun showLoadingSubmissionsPanel(platformName: String) {}

  override fun showLoadingCommunityPanel(platformName: String) {}

  override fun readyToCheck() {}
  override fun checkStarted(task: Task, startSpinner: Boolean) {}
  override fun checkFinished(task: Task, checkResult: CheckResult) {}
}
