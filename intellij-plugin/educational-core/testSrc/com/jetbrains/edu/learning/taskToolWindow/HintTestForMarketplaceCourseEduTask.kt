package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.taskToolWindow.ui.getHintIconSize
import org.junit.Test

abstract class HintsInTaskDescriptionTestBase(
  private val isTheoryTask: Boolean, private val isHyperskillCourse: Boolean
) : TaskDescriptionTestBase() {

  override fun createCourseWithTestTask(taskDescription: String, format: DescriptionFormat) {
    val courseProducer = if (isHyperskillCourse) ::HyperskillCourse else ::EduCourse

    courseWithFiles(courseProducer = courseProducer, language = language, environment = environment) {
      lesson {
        if (isTheoryTask) {
          theoryTask(taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
        }
        else {
          eduTask(taskDescription = taskDescription.trimIndent(), taskDescriptionFormat = format)
        }
      }
    }
  }

  override fun removeUnimportantParts(html: String): String = super.removeUnimportantParts(html)
    // inside <div class='hint'><img src="...">, ignore paths to bulb.png and to right.png
    .replace(Regex("""src="[^"]*(bulb.png|right.png)""""), """src="$1"""")

  companion object {
    const val HINT_DIV = """
      <div class='hint'>Hint text</div>
    """

    const val JCEF_PROCESSED_HINT_DIV = """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint
          </div>
          <div class="hint_content">
           Hint text
          </div>
         </div>
        </div>
       </body>
      </html>
    """

    fun swingProcessedHintDiv() = """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <div class="top">
          <div class="hint">
           <img src="" width="0" height="0"> <span><a href="hint://" value="Hint text">Hint </a> <span class="chevron">›</span></span>
          </div>
         </div>
        </div>
       </body>
      </html>
    """.replaceWidthHeightWithFontSize()

    const val UNPROCESSED_HINT_DIV = """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <div class="hint">
          Hint text
         </div>
        </div>
       </body>
      </html>
    """

    fun String.replaceWidthHeightWithFontSize(): String {
      val iconSize = getHintIconSize()
      return replace(Regex("""(width|height)="0""""), """$1="$iconSize"""")
    }
  }
}

class HintTestForHyperskillCourseEduTask : HintsInTaskDescriptionTestBase(false, true) {

  @Test
  fun `test hints are not processed in hyperskill edu tasks`() = doMarkdownTest(HINT_DIV, UNPROCESSED_HINT_DIV)
}

class HintTestForHyperskillCourseTheoryTask : HintsInTaskDescriptionTestBase(true, true) {

  @Test
  fun `test hints are not processed in hyperskill theory tasks`() = doMarkdownTest(HINT_DIV, UNPROCESSED_HINT_DIV)
}

class HintTestForMarketplaceCourseTheoryTask : HintsInTaskDescriptionTestBase(true, false) {

  @Test
  fun `test hints are processed in marketplace theory tasks`() = doMarkdownTest(HINT_DIV, JCEF_PROCESSED_HINT_DIV, swingProcessedHintDiv())
}

class HintTestForMarketplaceCourseEduTask : HintsInTaskDescriptionTestBase(false, false) {

  @Test
  fun `test hints are processed in marketplace edu tasks`() = doMarkdownTest(HINT_DIV, JCEF_PROCESSED_HINT_DIV, swingProcessedHintDiv())

  @Test
  fun `test hints with different titles numbering`() = doMarkdownTest(
    """
      Hello
      <div class="hint" title="Explanation">Text 1</div>
      <div class="hint" title="Clarification">Text 2</div>
      <div class="hint">Text 3</div>
      <div class="hint" title="Clarification">Text 4</div>
      <div class="hint" title="Random title">Text 5</div>
      <div class="hint">Text 6</div>
      <div class="hint" title="Explanation">Text 7</div>
    """, """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <p>Hello</p>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Explanation 1
          </div>
          <div class="hint_content">
           Text 1
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Clarification 1
          </div>
          <div class="hint_content">
           Text 2
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint 1
          </div>
          <div class="hint_content">
           Text 3
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Clarification 2
          </div>
          <div class="hint_content">
           Text 4
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Random title
          </div>
          <div class="hint_content">
           Text 5
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint 2
          </div>
          <div class="hint_content">
           Text 6
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Explanation 2
          </div>
          <div class="hint_content">
           Text 7
          </div>
         </div>
        </div>
       </body>
      </html>
    """, """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <p>Hello</p>
         <div class="top">
          <div class="hint">
           <img src="" width="0" height="0"> <span><a href="hint://1" value="Text 1">Explanation 1</a> <span class="chevron">›</span></span>
          </div>
         </div>
         <div class="top">
          <div class="hint">
           <img src="" width="0" height="0"> <span><a href="hint://1" value="Text 2">Clarification 1</a> <span class="chevron">›</span></span>
          </div>
         </div>
         <div class="top">
          <div class="hint">
           <img src="" width="0" height="0"> <span><a href="hint://1" value="Text 3">Hint 1</a> <span class="chevron">›</span></span>
          </div>
         </div>
         <div class="hint">
          <img src="" width="0" height="0"> <span><a href="hint://2" value="Text 4">Clarification 2</a> <span class="chevron">›</span></span>
         </div>
         <div class="top">
          <div class="hint">
           <img src="" width="0" height="0"> <span><a href="hint://" value="Text 5">Random title </a> <span class="chevron">›</span></span>
          </div>
         </div>
         <div class="hint">
          <img src="" width="0" height="0"> <span><a href="hint://2" value="Text 6">Hint 2</a> <span class="chevron">›</span></span>
         </div>
         <div class="hint">
          <img src="" width="0" height="0"> <span><a href="hint://2" value="Text 7">Explanation 2</a> <span class="chevron">›</span></span>
         </div>
        </div>
       </body>
      </html>
    """.replaceWidthHeightWithFontSize()
  )

  @Test
  fun `first-paragraph CSS class is added to the first paragraph if there is no text before it`() = doMarkdownTest(
    """
      <div class="hint">Text in **HTML** mode</div>
      <div class="hint">
        Text in **HTML** mode

        Text in **Markdown** mode
      </div>
      <div class="hint">
      
        Text in **Markdown** mode
        
        Text in **Markdown** mode
      </div>
      <div class="hint"><p>Text in **HTML** mode</p></div>
      <div class="hint">  <!--no text-->  <p>Text in **HTML** mode</p></div>
    """, """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint 1
          </div>
          <div class="hint_content">
           Text in **HTML** mode
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint 2
          </div>
          <div class="hint_content">
           Text in **HTML** mode 
           <p>Text in <strong>Markdown</strong> mode</p>
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint 3
          </div>
          <div class="hint_content">
           <p class="first-paragraph">Text in <strong>Markdown</strong> mode</p>
           <p>Text in <strong>Markdown</strong> mode</p>
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint 4
          </div>
          <div class="hint_content">
           <p class="first-paragraph">Text in **HTML** mode</p>
          </div>
         </div>
         <div class="hint">
          <div class="hint_header">
           <img src="" style="display: inline-block;"> Hint 5
          </div>
          <div class="hint_content">
           <p class="first-paragraph">Text in **HTML** mode</p>
          </div>
         </div>
        </div>
       </body>
      </html>
    """, """
      <html>
       <head>
        ...
       </head>
       <body>
        <div class="wrapper">
         <div class="top">
          <div class="hint">
           <img src="" width="0" height="0"> <span><a href="hint://1" value="Text in **HTML** mode">Hint 1</a> <span class="chevron">›</span></span>
          </div>
         </div>
         <div class="hint">
          <img src="" width="0" height="0"> <span><a href="hint://2" value="Text in **HTML** mode Text in Markdown mode">Hint 2</a> <span class="chevron">›</span></span>
         </div>
         <div class="hint">
          <img src="" width="0" height="0"> <span><a href="hint://3" value="Text in Markdown mode Text in Markdown mode">Hint 3</a> <span class="chevron">›</span></span>
         </div>
         <div class="hint">
          <img src="" width="0" height="0"> <span><a href="hint://4" value="Text in **HTML** mode">Hint 4</a> <span class="chevron">›</span></span>
         </div>
         <div class="hint">
          <img src="" width="0" height="0"> <span><a href="hint://5" value="Text in **HTML** mode">Hint 5</a> <span class="chevron">›</span></span>
         </div>
        </div>
       </body>
      </html>
    """.replaceWidthHeightWithFontSize()
  )
}