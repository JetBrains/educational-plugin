package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class MockTaskDescriptionView : TaskDescriptionView() {
  override var currentTask: Task?
    get() = null
    set(value) {}

  override fun init(toolWindow: ToolWindow) {}
  override fun updateTaskSpecificPanel() {}
  override fun updateTaskDescription(task: Task?) {}
  override fun updateTaskDescription() {}
  override fun updateAdditionalTaskTab() {}
  override fun readyToCheck() {}
  override fun checkStarted() {}
  override fun checkFinished(task: Task, checkResult: CheckResult) {}
  override fun showBalloon(text: String, messageType: MessageType) {}
}
