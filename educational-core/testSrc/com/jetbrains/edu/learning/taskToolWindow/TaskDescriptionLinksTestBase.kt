package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.taskToolWindow.ui.ToolWindowLinkHandler

abstract class TaskDescriptionLinksTestBase : EduTestCase() {

  protected fun openLink(link: String) {
    ToolWindowLinkHandler(project).process(link)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}
