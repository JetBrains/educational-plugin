package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlUIMode
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.pipeline
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps.CodeHighlighter
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps.ExternalLinkIconsTransformer
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps.MediaThemesTransformer
import org.intellij.lang.annotations.Language

abstract class TaskDescriptionHighlightingTestBase : EduTestCase() {

  protected abstract val language: com.intellij.lang.Language
  open val environment: String = ""

  private val codeHighlighterTransformer = pipeline(
    MediaThemesTransformer,
    ExternalLinkIconsTransformer,
    CodeHighlighter
  ).toStringTransformer()

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

    val actualText = TaskDescriptionToolWindow.getTaskDescription(project, task)

    fun testForSpecificUIMode(uiMode: HtmlUIMode) {
      val transformationContext = HtmlTransformerContext(project, task, uiMode)
      val html = codeHighlighterTransformer.transform(actualText, transformationContext)
      assertEquals(expectedText.trimIndent(), html.dropSpecificValues())
    }

    testForSpecificUIMode(HtmlUIMode.SWING)
    testForSpecificUIMode(HtmlUIMode.JCEF)
  }

  private fun String.dropSpecificValues(): String = lines()
    .joinToString("\n", transform = String::trimEnd)
    .replace(SPAN_REGEX, """<span style="...">""")

  companion object {
    private val SPAN_REGEX = Regex("""<span style=".*?">""")
  }
}
