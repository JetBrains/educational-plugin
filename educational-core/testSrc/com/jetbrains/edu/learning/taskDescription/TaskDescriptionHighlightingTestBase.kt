package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow
import org.intellij.lang.annotations.Language

abstract class TaskDescriptionHighlightingTestBase : EduTestCase() {

  protected abstract val language: com.intellij.lang.Language
  open val environment: String = ""
  protected abstract val settings: Any

  override fun doRunTests() {
    if (ApplicationInfo.getInstance().build >= BUILD_183) {
      super.doRunTests()
    }
  }

  protected fun doHtmlTest(@Language("HTML") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.HTML, expectedText)

  protected fun doMarkdownTest(@Language("Markdown") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.MD, expectedText)

  protected fun doTest(
    taskDescription: String,
    format: DescriptionFormat,
    @Language("HTML") expectedText: String
  ) {
    val course = courseWithFiles(language = language, environment = environment, settings = settings) {
      lesson("lesson1") {
        eduTask("task1", taskDescription.trimIndent(), format)
      }
    }

    val task = course.findTask("lesson1", "task1")
    val actualText = TaskDescriptionToolWindow.getTaskDescriptionWithCodeHighlighting(project, task)
    assertEquals(expectedText.trimIndent(), actualText.dropSpecificValues())
  }

  private fun String.dropSpecificValues(): String = lines()
    .joinToString("\n", transform = String::trimEnd)
    .replace (SPAN_REGEX, """<span style="...">""")

  companion object {
    private val SPAN_REGEX = Regex("""<span style=".*?">""")
    private val BUILD_183 = BuildNumber.fromString("183")
  }
}
