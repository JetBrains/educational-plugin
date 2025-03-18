package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion
import org.junit.Test

abstract class TermsInTaskDescriptionTestBase(
  private val termTitles: List<String>,
  private val isTheoryTask: Boolean = true,
) : TaskDescriptionTestBase() {
  override fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    courseWithFiles(language = language, environment = environment) {
      lesson {
        if (isTheoryTask) {
          theoryTask(taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
        }
        else {
          eduTask(taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
        }
      }
    }
    val termsProperties = TermsProperties(
      languageCode = TranslationLanguage.ENGLISH.code,
      terms = mapOf(0 to termTitles.map { Term(it, "") }),
      version = TermsVersion(1),
    )
    TermsProjectSettings.getInstance(project).setTerms(termsProperties)
  }
}

@Suppress("HtmlRequiredTitleElement")
class TermsInEduTaskDescriptionTest : TermsInTaskDescriptionTestBase(
  listOf("thread", "green thread", "mem"),
  isTheoryTask = false
) {
  @Test
  fun `test terms do not appear in edu task`() {
    doHtmlTest(
      """
        hello!
        thread
        green thread
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           hello! thread green thread
          </div>
         </body>
        </html>
      """
    )

    doMarkdownTest(
      """
        hello!
        thread
        green thread
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p>hello! thread green thread</p>
          </div>
         </body>
        </html>
      """
    )
  }
}

@Suppress("HtmlRequiredTitleElement")
class TermsInTheoryTaskDescriptionTest : TermsInTaskDescriptionTestBase(
  listOf("thread", "green thread", "mem")
) {
  @Test
  fun `test terms highlight only once in task html description`() {
    doHtmlTest(
      """
        <html>
        <p>mem mem</p>
        </html>
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p><span style="border-bottom: 1px dashed gray;" class="term">mem</span> mem</p>
          </div>
         </body>
        </html>
      """
    )

    doMarkdownTest(
      """
        mem mem
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p><span style="border-bottom: 1px dashed gray;" class="term">mem</span> mem</p>
          </div>
         </body>
        </html>
      """
    )
  }

  @Test
  fun `test terms do not highlight inside another term`() {
    doHtmlTest(
      """
        <html>
        <p>hello! green thread</p>
        <p>thread</p>
        </html>
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p>hello! <span style="border-bottom: 1px dashed gray;" class="term">green thread</span></p>
           <p><span style="border-bottom: 1px dashed gray;" class="term">thread</span></p>
          </div>
         </body>
        </html>
      """
    )

    doMarkdownTest(
      """
        hello! green thread
        
        thread
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p>hello! <span style="border-bottom: 1px dashed gray;" class="term">green thread</span></p>
           <p><span style="border-bottom: 1px dashed gray;" class="term">thread</span></p>
          </div>
         </body>
        </html>
      """
    )
  }

  @Test
  fun `test terms do not highlight if they are a substring of another word`() {
    doHtmlTest(
      """
        <html>
        <p>threading</p>
        <p>wthread</p>
        </html>
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p>threading</p>
           <p>wthread</p>
          </div>
         </body>
        </html>
      """
    )

    doMarkdownTest(
      """
        threading
        
        wthread
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p>threading</p>
           <p>wthread</p>
          </div>
         </body>
        </html>
      """
    )
  }

  @Test
  fun `test multiple terms can highlight in the same node`() {
    doHtmlTest(
      """
        <html>
        <p>hello! green thread thread</p>
        </html>
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p>hello! <span style="border-bottom: 1px dashed gray;" class="term">green thread</span> <span style="border-bottom: 1px dashed gray;" class="term">thread</span></p>
          </div>
         </body>
        </html>
      """
    )

    doMarkdownTest(
      """
        hello! green thread thread
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p>hello! <span style="border-bottom: 1px dashed gray;" class="term">green thread</span> <span style="border-bottom: 1px dashed gray;" class="term">thread</span></p>
          </div>
         </body>
        </html>
      """
    )
  }

  @Suppress("HtmlUnknownTarget")
  @Test
  fun `test terms do not highlight inside links and images`() {
    doHtmlTest(
      """
        <html>
        <p><a href="TODO.html">thread</a></p>
        <p><img src="https://dark.png" alt="thread"></p>
        </html>
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p><a href="TODO.html">thread</a></p>
           <p><img src="https://dark.png" alt="thread"></p>
          </div>
         </body>
        </html>
      """
    )

    doMarkdownTest(
      """
        [thread](thread "thread")
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <p><a href="thread" title="thread">thread</a></p>
          </div>
         </body>
        </html>
      """
    )
  }

  @Test
  fun `test terms do not highlight inside code blocks`() {
    doHtmlTest(
      """
        <html>
        <pre><code>
          thread
        </code></pre>
        <code>green thread</code>
        </html>
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <span class="code-block">
            <pre>  <span style="color: #000000;">thread</span>
        </pre></span> <span class="code"><span style="color: #000000;">green thread</span></span>
          </div>
         </body>
        </html>
      """
    )

    doMarkdownTest(
      """
        ```
        thread
        ```
        
        `green thread`
      """,
      """
        <html>
         <head>
          ...
         </head>
         <body>
          <div class="wrapper">
           <span class="code-block">
            <pre><span style="color: #000000;">thread</span>
        </pre></span>
           <p><span class="code"><span style="color: #000000;">green thread</span></span></p>
          </div>
         </body>
        </html>
      """
    )
  }
}
