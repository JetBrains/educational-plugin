package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

abstract class EduSettingsIdCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {
  fun `test complete Educational settings id`() = doTest("settings://Edu<caret>", "settings://Educational<caret>")
}
