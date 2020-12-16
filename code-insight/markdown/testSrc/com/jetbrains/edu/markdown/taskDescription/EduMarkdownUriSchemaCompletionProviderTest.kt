package com.jetbrains.edu.markdown.taskDescription

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.codeInsight.taskDescription.EduUriSchemaCompletionProviderTestBase
import com.jetbrains.edu.learning.TestContext
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.intellij.lang.annotations.Language

class EduMarkdownUriSchemaCompletionProviderTest : EduUriSchemaCompletionProviderTestBase() {

  override val taskDescriptionFormat: DescriptionFormat get() = DescriptionFormat.MD

  fun `test course schema`() = doTest("""
    [link text](cou<caret>)
  """, """
    [link text](course://<caret>)
  """)

  fun `test psi_element schema`() = doTest("""
    [link text](psi<caret>)
  """, """
    [link text](psi_element://<caret>)
  """)

  // Overrides basic method just to add @Language annotation to provide language injection in test string literals
  @Suppress("RedundantOverride")
  override fun doTest(@Language("Markdown") before: String, @Language("Markdown") after: String) {
    super.doTest(before, after)
  }
}
