package com.jetbrains.edu.markdown.taskDescription

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.coursecreator.taskDescription.EduUriSchemaCompletionProviderTestBase
import com.jetbrains.edu.learning.TestContext
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.intellij.lang.annotations.Language

class EduMarkdownUriSchemaCompletionProviderTest : EduUriSchemaCompletionProviderTestBase() {

  override val taskDescriptionFormat: DescriptionFormat get() = DescriptionFormat.MD

  override fun runTestInternal(context: TestContext) {
    // Markdown plugin for 201 tries to load JavaFX on editor opening and fails
    // BACKCOMPAT: 2020.1
    if (ApplicationInfo.getInstance().build >= BUILD_202) {
      super.runTestInternal(context)
    }
  }

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

  companion object {
    // BACKCOMPAT: 2020.1
    private val BUILD_202: BuildNumber = BuildNumber.fromString("202")!!
  }
}
