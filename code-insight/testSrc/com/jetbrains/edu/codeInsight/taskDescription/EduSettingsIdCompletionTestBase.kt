package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

abstract class EduSettingsIdCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionCompletionTestBase(format) {
  fun `test complete Educational settings`() = doTest("settings://Edu<caret>", "settings://Educational<caret>")
  fun `test complete Menus And Toolbars settings by display name`() = doTest("settings://menu<caret>", "settings://preferences.customizations<caret>")
  fun `test complete Appearance settings by id`() = doTest("settings://Look<caret>", "settings://preferences.lookFeel<caret>")
}
