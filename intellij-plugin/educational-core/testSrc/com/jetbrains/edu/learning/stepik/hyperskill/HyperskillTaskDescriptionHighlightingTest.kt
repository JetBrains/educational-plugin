package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import org.junit.Test

abstract class HyperskillTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  protected abstract val codeSample: String
  protected abstract val codeSampleWithHighlighting: String

  protected open val codeSampleWithBasicHighlighting: String
    get() = """<span style="...">$codeSample</span>"""

  override fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    courseWithFiles(courseProducer = ::HyperskillCourse, language = language, environment = environment) {
      lesson {
        eduTask(taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
      }
    }
  }

  @Test
  fun `test class no-highlighting in hyperskill course`() {
    doHtmlTest("""
      <html>
       <body>
        <code class="language-no-highlight">$codeSample</code>
       </body>
      </html>
    """, """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <span class="code">$codeSampleWithBasicHighlighting</span>
        </div>
       </body>
      </html>
    """)
  }

  @Test
  fun `test no language in hyperskill course`() {
    doHtmlTest("""
      <html>
       <body>
        <code>$codeSample</code>
       </body>
      </html>
    """, """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <span class="code">$codeSampleWithBasicHighlighting</span>
        </div>
       </body>
      </html>
    """)
  }

  @Test
  fun `test highlighting`() {
    doHtmlTest("""
      <html>
       <body>
        <code class="language-${language.id.lowercase()}">$codeSample</code>
       </body>
      </html>
    """, """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <span class="code">$codeSampleWithHighlighting</span>
        </div>
       </body>
      </html>
    """)
  }
}
