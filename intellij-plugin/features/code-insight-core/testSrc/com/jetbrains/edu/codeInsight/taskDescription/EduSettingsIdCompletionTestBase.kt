package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.junit.Test

abstract class EduSettingsIdCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {
  @Test
  fun `test complete Educational settings`() = doTest("settings://Edu<caret>", "settings://Educational<caret>")
  @Test
  fun `test complete Menus And Toolbars settings by display name`() = doTest("settings://menu<caret>", "settings://preferences.customizations<caret>")
  @Test
  fun `test complete Appearance settings by id`() = doTest("settings://Look<caret>", "settings://preferences.lookFeel<caret>")
}
