package com.jetbrains.edu.html.taskDescription

import com.jetbrains.edu.coursecreator.taskDescription.EduUriSchemaCompletionProviderTestBase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.intellij.lang.annotations.Language

@Suppress("HtmlUnknownTarget")
class EduHtmlUriSchemaCompletionProviderTest : EduUriSchemaCompletionProviderTestBase() {

  override val taskDescriptionFormat: DescriptionFormat get() = DescriptionFormat.HTML

  fun `test course schema`() = doTest("""
    <a href='cour<caret>'>link text</a>
  """, """
    <a href='course://<caret>'>link text</a>
  """)

  fun `test psi_element schema`() = doTest("""
    <a href='psi<caret>'>link text</a>
  """, """
    <a href='psi_element://<caret>'>link text</a>
  """)

  // Overrides basic method just to add @Language annotation to provide language injection in test string literals
  @Suppress("RedundantOverride")
  override fun doTest(@Language("HTML") before: String, @Language("HTML") after: String) = super.doTest(before, after)
}
