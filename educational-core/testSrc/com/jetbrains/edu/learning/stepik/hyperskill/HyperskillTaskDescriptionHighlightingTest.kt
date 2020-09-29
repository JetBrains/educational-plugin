package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase

abstract class HyperskillTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  protected abstract val codeSample: String
  protected abstract val codeSampleWithHighlighting: String

  protected open val codeSampleWithBasicHighlighting: String
    get() = """<span style="...">$codeSample</span>"""

  override fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    courseWithFiles(courseProducer = ::HyperskillCourse, language = language, environment = environment, settings = settings) {
      lesson {
        eduTask(taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
      }
    }
  }

  fun `test class no-highlighting in hyperskill course`() {
    doHtmlTest("""
      <html>
       <body>
        <code class="language-no-highlight">$codeSample</code>
       </body>
      </html>
    """, """
      <html>
       <head></head>
       <body>
        <span class="code">$codeSampleWithBasicHighlighting</span>
       </body>
      </html>
        """)
  }

  fun `test no language in hyperskill course`() {
    doHtmlTest("""
      <html>
       <body>
        <code>$codeSample</code>
       </body>
      </html>
    """, """
      <html>
       <head></head>
       <body>
        <span class="code">$codeSampleWithBasicHighlighting</span>
       </body>
      </html>
        """)
  }

  fun `test highlighting`() {
    doHtmlTest("""
      <html>
       <body>
        <code class="language-${language.id.toLowerCase()}">$codeSample</code>
       </body>
      </html>
    """, """
      <html>
       <head></head>
       <body>
        <span class="code">${codeSampleWithHighlighting}</span>
       </body>
      </html>
        """)
  }
}
