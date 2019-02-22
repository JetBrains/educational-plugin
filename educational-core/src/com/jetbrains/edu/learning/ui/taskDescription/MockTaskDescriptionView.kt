package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.Point

class MockTaskDescriptionView : TaskDescriptionView() {
  override var currentTask: Task?
    get() = null
    set(_) {}

  override fun init(toolWindow: ToolWindow) {}
  override fun updateTaskSpecificPanel() {}
  override fun updateTaskDescription(task: Task?) {}
  override fun updateTaskDescription() {}
  override fun updateAdditionalTaskTab() {}
  override fun readyToCheck() {}
  override fun checkStarted() {}
  override fun checkFinished(task: Task, checkResult: CheckResult) {}
  override fun checkTooltipPosition(): RelativePoint = RelativePoint(Point(0, 0))
}
