package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow
import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Element
import org.jsoup.select.NodeFilter.FilterResult.CONTINUE
import org.jsoup.select.NodeFilter.FilterResult.REMOVE

abstract class TaskDescriptionTestBase : EduTestCase() {

  protected open val language: com.intellij.lang.Language = PlainTextLanguage.INSTANCE
  protected open val environment: String = ""

  protected fun doHtmlTest(
    @Language("HTML") taskDescriptionText: String,
    @Language("HTML") expectedJCEFHtml: String,
    @Language("HTML") expectedSwingHtml: String = expectedJCEFHtml
  ) = doTest(taskDescriptionText, DescriptionFormat.HTML, expectedJCEFHtml, expectedSwingHtml)

  protected fun doMarkdownTest(
    @Language("Markdown") taskDescriptionText: String,
    @Language("HTML") expectedJCEFHtml: String,
    @Language("HTML") expectedSwingHtml: String = expectedJCEFHtml
  ) = doTest(taskDescriptionText, DescriptionFormat.MD, expectedJCEFHtml, expectedSwingHtml)

  private fun doTest(
    taskDescriptionText: String,
    format: DescriptionFormat,
    expectedJCEFHtml: String,
    expectedSwingHtml: String = expectedJCEFHtml
  ) {
    createCourseWithTestTask(taskDescriptionText, format)
    val task = findTask(0, 0)

    fun testForSpecificUIMode(uiMode: JavaUILibrary, expectedHtml: String) {
      val html = removeUnimportantParts(TaskDescriptionToolWindow.getTaskDescription(project, task, uiMode))
      assertEquals("Unexpected task description html for $uiMode rendering mode", expectedHtml.trimIndent(), html)
    }

    testForSpecificUIMode(JavaUILibrary.JCEF, expectedJCEFHtml)
    testForSpecificUIMode(JavaUILibrary.SWING, expectedSwingHtml)
  }

  protected open fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    courseWithFiles(language = language, environment = environment) {
      lesson("lesson1") {
        eduTask("task1", taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
      }
    }
  }

  /**
   * Removes some parts of the final task description html which are not important for particular tests
   */
  protected open fun removeUnimportantParts(html: String): String {
    val document = Jsoup.parse(html)
    // We don't care about comments
    document.filter { node, _ ->
      if (node is Comment) REMOVE else CONTINUE
    }
    // `ResourceWrapper` adds a lot of resources to `head` section.
    // In most cases, we don't want to check them
    document.selectFirst("head")?.replaceWith(Element("head").text("..."))
    return document.toString()
  }
}