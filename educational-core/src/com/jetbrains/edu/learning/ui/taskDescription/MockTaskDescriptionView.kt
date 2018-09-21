package com.jetbrains.edu.learning.ui.taskDescription

import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class MockTaskDescriptionView : TaskDescriptionView() {
  override var currentTask: Task?
    get() = null
    set(value) {}

  override fun init() {}
  override fun updateTaskSpecificPanel() {}
  override fun updateTaskDescription(task: Task?) {}
  override fun updateTaskDescription() {}
  override fun readyToCheck() {}
  override fun checkStarted() {}
  override fun checkFinished(checkResult: CheckResult) {}
  override fun dispose() {}
}
