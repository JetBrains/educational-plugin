package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.EduTestCase

abstract class TaskDescriptionLinksTestBase : EduTestCase() {

  protected fun openLink(link: String) {
    ToolWindowLinkHandler(project).process(link)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}
