package com.jetbrains.edu.learning.placeholder

import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class PlaceholderHighlightingPassTest : EduTestCase() {

  override val useDocumentListener: Boolean = false

  @Test
  fun `test unchecked placeholder highlighting`() = doTest("""
    <p>oneline placeholder 1</p> some text
    <p>oneline placeholder 2</p> some text
  """, status = CheckStatus.Unchecked)

  @Test
  fun `test failed placeholder highlighting`() = doTest("""
    <p>multiline
       placeholder 1</p>
    <p>multi
       line
       placeholder 2</p>       
  """, status = CheckStatus.Failed)

  @Test
  fun `test solved placeholder highlighting`() = doTest("""
    empty <p></p> placeholders <p></p>
  """, status = CheckStatus.Solved)

  @Test
  fun `test placeholder highlighting in educator mode`() = doTest("""
    <p>oneline placeholder</p> some text  
    <p>multiline
       placeholder</p>
    empty <p></p> placeholder 
  """, courseMode = CourseMode.EDUCATOR)

  private fun doTest(
    text: String,
    status: CheckStatus = CheckStatus.Unchecked,
    courseMode: CourseMode = CourseMode.STUDENT,
  ) {
    val course = courseWithFiles(courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("taskFile1.txt", text.trimIndent()) {
            for (placeholder in taskFile.answerPlaceholders) {
              placeholder.possibleAnswer = placeholder.placeholderText
            }
          }
        }
      }
    }

    val task = course.findTask("lesson1", "task1")
    task.status = status

    val firstPlaceholder = task.taskFiles["taskFile1.txt"]!!.answerPlaceholders[0]
    val expectedSeverity = PlaceholderHighlightingInfo.forPlaceholder(firstPlaceholder).severity

    val file = findFile("lesson1/task1/taskFile1.txt")

    val textWithMarkup = text.trimIndent()
      .replace("<p></p>", "<eol_${expectedSeverity.name}></eol_${expectedSeverity.name}>")
      .replace("<p>", "<${expectedSeverity.name}>")
      .replace("</p>", "</${expectedSeverity.name}>")

    myFixture.saveText(file, textWithMarkup)
    myFixture.openFileInEditor(file)

    val highlightingData = ExpectedHighlightingData(myFixture.editor.document)
    for (info in PlaceholderHighlightingInfo.entries) {
      highlightingData.registerHighlightingType(info.severity.name, ExpectedHighlightingData.ExpectedHighlightingSet(info.severity, false, true))
      highlightingData.registerHighlightingType("eol_" + info.severity.name, ExpectedHighlightingData.ExpectedHighlightingSet(info.severity, true, true))
    }
    highlightingData.init()
    (myFixture as CodeInsightTestFixtureImpl).collectAndCheckHighlighting(highlightingData)
  }
}
