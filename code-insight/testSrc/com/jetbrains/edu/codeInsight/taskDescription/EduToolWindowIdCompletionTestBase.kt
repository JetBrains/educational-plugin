package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

abstract class EduToolWindowIdCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {
  fun `test complete tool window id`() = doTest("tool_window://Tas<caret>", "tool_window://Task<caret>")
}
