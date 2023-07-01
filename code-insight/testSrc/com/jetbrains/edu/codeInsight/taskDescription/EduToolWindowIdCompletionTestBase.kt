package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory

abstract class EduToolWindowIdCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {

  override fun setUp() {
    super.setUp()
    registerTaskDescriptionToolWindow()
  }

  // In tests, tool windows are not registered by default
  // Let's register at least Task Description tool window
  private fun registerTaskDescriptionToolWindow() {
    val toolWindowManager = ToolWindowManager.getInstance(project) as ToolWindowHeadlessManagerImpl
    if (toolWindowManager.getToolWindow("Task") == null) {
      for (bean in ToolWindowEP.EP_NAME.extensionList) {
        if (bean.id == TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW) {
          toolWindowManager.doRegisterToolWindow(bean.id)
          Disposer.register(testRootDisposable) {
            @Suppress("DEPRECATION")
            toolWindowManager.unregisterToolWindow(bean.id)
          }
        }
      }
    }
  }

  fun `test complete tool window id`() = doTest("tool_window://Tas<caret>", "tool_window://Task<caret>")
}
