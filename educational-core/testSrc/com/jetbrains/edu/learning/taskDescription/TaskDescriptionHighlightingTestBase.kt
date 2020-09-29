package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow
import org.intellij.lang.annotations.Language

abstract class TaskDescriptionHighlightingTestBase : EduTestCase() {

  protected abstract val language: com.intellij.lang.Language
  open val environment: String = ""
  protected abstract val settings: Any

  private var oldColorScheme: EditorColorsScheme? = null

  override fun setUp() {
    super.setUp()
    // BACKCOMPAT: 2020.2.
    //  Since 2020.3 `HTMLTextPainter.convertCodeFragmentToHTMLFragmentWithInlineStyles` that we use in `EduCodeHighlighter`
    //  adds `span` attribute for all identifiers that have "default" color (see `HtmlStyleManager.isDefaultAttributes`).
    //  To avoid different tests for all supported platform,
    //  dark editor theme is set up here to force `HTMLTextPainter` add `span` attribute for all identifiers on all platforms
    val colorsManager = EditorColorsManager.getInstance()
    val oldColorScheme = colorsManager.globalScheme
    val darkScheme = colorsManager.allSchemes.find {
      // Copied from `com.intellij.openapi.editor.colors.EditorColorsManager.isDarkEditor`
      ColorUtil.isDark(it.defaultBackground)
    } ?: oldColorScheme
    colorsManager.globalScheme = darkScheme
  }

  override fun tearDown() {
    oldColorScheme?.let {
      EditorColorsManager.getInstance().globalScheme = it
    }
    super.tearDown()
  }

  protected fun doHtmlTest(@Language("HTML") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.HTML, expectedText)

  protected fun doMarkdownTest(@Language("Markdown") taskDescription: String, @Language("HTML") expectedText: String) =
    doTest(taskDescription, DescriptionFormat.MD, expectedText)

  protected open fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    courseWithFiles(language = language, environment = environment, settings = settings) {
      lesson("lesson1") {
        eduTask("task1", taskDescription.trimIndent(), format)
      }
    }
  }

  private fun doTest(taskDescription: String,
                     format: DescriptionFormat,
                     @Language("HTML") expectedText: String) {
    createCourseWithTestTask(taskDescription, format)
    val task = findTask(0, 0)
    val actualText = TaskDescriptionToolWindow.getTaskDescriptionWithCodeHighlighting(project, task)
    assertEquals(expectedText.trimIndent(), actualText.dropSpecificValues())
  }

  private fun String.dropSpecificValues(): String = lines()
    .joinToString("\n", transform = String::trimEnd)
    .replace(SPAN_REGEX, """<span style="...">""")

  companion object {
    private val SPAN_REGEX = Regex("""<span style=".*?">""")
  }
}
