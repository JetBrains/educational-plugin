package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.CodeHighlighter
import org.intellij.lang.annotations.Language

abstract class TaskDescriptionHighlightingTestBase : EduTestCase() {

  protected abstract val language: com.intellij.lang.Language
  open val environment: String = ""
  protected abstract val settings: Any

  protected fun doHtmlTest(@Language("HTML") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.HTML, expectedText)

  protected fun doMarkdownTest(@Language("Markdown") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.MD, expectedText)

  protected open fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    courseWithFiles(language = language, environment = environment, settings = settings) {
      lesson("lesson1") {
        eduTask("task1", taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
      }
    }
  }

  private fun doTest(taskDescription: String,
                     format: DescriptionFormat,
                     @Language("HTML") expectedText: String) {
    createCourseWithTestTask(taskDescription, format)
    val task = findTask(0, 0)
    val html = EduUtils.getTaskTextFromTask(project, task) ?: ""
    val actualText = CodeHighlighter.swingTransform(html, HtmlTransformerContext(project, task))
    assertEquals(expectedText.trimIndent(), actualText.dropSpecificValues())
  }

  private fun String.dropSpecificValues(): String = lines()
    .joinToString("\n", transform = String::trimEnd)
    .replace(SPAN_REGEX, """<span style="...">""")

  companion object {
    private val SPAN_REGEX = Regex("""<span style=".*?">""")
  }
}
