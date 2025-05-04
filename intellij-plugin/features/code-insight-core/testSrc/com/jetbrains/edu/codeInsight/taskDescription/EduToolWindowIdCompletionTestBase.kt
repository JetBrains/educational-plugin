package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.junit.Test

abstract class EduToolWindowIdCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {

  override fun setUp() {
    super.setUp()
    registerToolWindow(LIBRARY_TOOL_WINDOW_ID)
    registerToolWindow(FACET_TOOL_WINDOW_ID)
  }

  @Test
  fun `test complete tool window id`() = doTest("tool_window://Tas<caret>", "tool_window://Task<caret>")
  @Test
  fun `test complete library tool window id`() = doTest("tool_window://${LIBRARY_TOOL_WINDOW_ID.dropLast(1)}<caret>", "tool_window://$LIBRARY_TOOL_WINDOW_ID<caret>")
  @Test
  fun `test complete facet tool window id`() = doTest("tool_window://${FACET_TOOL_WINDOW_ID.dropLast(1)}<caret>", "tool_window://$FACET_TOOL_WINDOW_ID<caret>")

  companion object {
    private const val LIBRARY_TOOL_WINDOW_ID = "edu.test.library.window"
    private const val FACET_TOOL_WINDOW_ID = "edu.test.facet.window"
  }
}
