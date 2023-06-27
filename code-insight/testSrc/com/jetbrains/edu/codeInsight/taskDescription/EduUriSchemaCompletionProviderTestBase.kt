package com.jetbrains.edu.codeInsight.taskDescription

abstract class EduUriSchemaCompletionProviderTestBase : EduTaskDescriptionCompletionTestBase() {
  fun `test course schema`() = doTest("cour<caret>", "course://<caret>")
  fun `test psi_element schema`() = doTest("psi<caret>", "psi_element://<caret>")
  fun `test settings schema`() = doTest("sett<caret>", "settings://<caret>")
  fun `test tool window schema`() = doTest("tool<caret>", "tool_window://<caret>")
}
