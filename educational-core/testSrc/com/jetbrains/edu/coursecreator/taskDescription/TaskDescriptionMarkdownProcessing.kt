package com.jetbrains.edu.coursecreator.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.wrapHintTagsInsideHTML
import junit.framework.TestCase
import org.jsoup.Jsoup

class TaskDescriptionMarkdownProcessing : EduTestCase() {

  fun `test hints with different titles numbering`() {
    val html = """
      Hello
      <div class="hint" title="Explanation">Text 1</div>
      <div class="hint" title="Clarification">Text 2</div>
      <div class="hint">Text 3</div>
      <div class="hint" title="Clarification">Text 4</div>
      <div class="hint" title="Random title">Text 5</div>
      <div class="hint">Text 6</div>
      <div class="hint" title="Explanation">Text 7</div>
    """.trimIndent()

    val wrappedHints = wrapHintTagsInsideHTML(Jsoup.parse(html)) { _, n, t -> "$t$n" }
    val hint = EduCoreBundle.message("course.creator.yaml.hint.default.title")

    TestCase.assertEquals("""
      <html>
       <head></head>
       <body>
        Hello 
        <div class="hint">
         Explanation1
        </div>
        <div class="hint">
         Clarification1
        </div>
        <div class="hint">
         ${hint}1
        </div>
        <div class="hint">
         Clarification2
        </div>
        <div class="hint">
         Random title
        </div>
        <div class="hint">
         ${hint}2
        </div>
        <div class="hint">
         Explanation2
        </div>
       </body>
      </html>
    """.trimIndent(), wrappedHints.toString())
  }

}