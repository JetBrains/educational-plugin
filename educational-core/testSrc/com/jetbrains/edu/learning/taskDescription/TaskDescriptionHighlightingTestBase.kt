package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.ext.getTaskTextFromTask
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps.CodeHighlighter
import org.intellij.lang.annotations.Language

abstract class TaskDescriptionHighlightingTestBase : EduTestCase() {

  protected abstract val language: com.intellij.lang.Language
  open val environment: String = ""

  protected fun doHtmlTest(@Language("HTML") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.HTML, expectedText)

  protected fun doMarkdownTest(@Language("Markdown") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.MD, expectedText)

  protected open fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    courseWithFiles(language = language, environment = environment) {
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

    val actualText = task.getTaskTextFromTask(project) ?: error("Failed to read task text")

    fun testForSpecificUIMode(uiMode: JavaUILibrary) {
      val transformationContext = HtmlTransformerContext(project, task, uiMode)
      val html = CodeHighlighter.toStringTransformer().transform(actualText, transformationContext)
      assertEquals(expectedText.trimIndent(), html.dropSpecificValues())
    }

    testForSpecificUIMode(JavaUILibrary.SWING)
    testForSpecificUIMode(JavaUILibrary.JCEF)
  }

  private fun String.dropSpecificValues(): String = lines()
    .joinToString("\n", transform = String::trimEnd)
    .replace(SPAN_REGEX, """<span style="...">""")

  companion object {
    private val SPAN_REGEX = Regex("""<span style=".*?">""")
  }
}
