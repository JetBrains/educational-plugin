package com.jetbrains.edu.learning

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.testFramework.EditorTestUtil
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import java.awt.Point

class PlaceholderPainterTest : EduTestCase() {

  fun `test inline placeholder`() {
    checkRectangular("""
    |This is <placeholder>one line </placeholder> placeholder
    """, LogicalPosition(0, 8), LogicalPosition(0, 17))
  }

  fun `test multi line rectangle`() {
    checkRectangular("""
    |def f():
    |<placeholder>    pass
    |    pass
    |    pass</placeholder>
    |
    |f()
    """, LogicalPosition(1, 0), LogicalPosition(3, 8))
  }

  fun `test last line longer`() {
    checkRectangular("""
      |<placeholder>test
      |testtest
      |testtesttest</placeholder>
    """, LogicalPosition(0, 0), LogicalPosition(2, 12))
  }

  fun `test first line longer`() {
    checkRectangular("""
      |<placeholder>testtesttest
      | testtest
      | test</placeholder>
    """, LogicalPosition(0, 0), LogicalPosition(2, 12))
  }

  fun `test one empty line`() {
    val expected = listOf(LogicalPositionInLine(0, 28),
                          LogicalPositionInLine(0, 28),
                          LogicalPositionInLine(1, 28, PositionInLine.BOTTOM),
                          LogicalPositionInLine(1, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 28, PositionInLine.BOTTOM))
    checkPath("""
    |fun checkSign(number: Int) =<placeholder>
    |    if (number > 0)</placeholder>
    """, expected)
  }

  fun `test with left margin`() {
    val expected = listOf(LogicalPositionInLine(0, 2),
                          LogicalPositionInLine(0, 4),
                          LogicalPositionInLine(1, 4, PositionInLine.BOTTOM),
                          LogicalPositionInLine(1, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 2, PositionInLine.BOTTOM))
    checkPath("""
     |t <placeholder>tt
     |  tt</placeholder>
    """, expected)
  }

  fun `test with right margin`() {
    val expected = listOf(LogicalPositionInLine(0, 0),
                          LogicalPositionInLine(0, 2),
                          LogicalPositionInLine(1, 2, PositionInLine.BOTTOM),
                          LogicalPositionInLine(1, 0, PositionInLine.BOTTOM))
    checkPath("""
     |<placeholder>tt
     |tt</placeholder> t
    """, expected)
  }

  fun `test complex shape`() {
    val expected = listOf(LogicalPositionInLine(0, 2),
                          LogicalPositionInLine(0, 4),
                          LogicalPositionInLine(0, 4, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 2, PositionInLine.BOTTOM),
                          LogicalPositionInLine(1, 2, PositionInLine.BOTTOM),
                          LogicalPositionInLine(1, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 2, PositionInLine.BOTTOM))
    checkPath("""
      |t <placeholder>tt
      |tt</placeholder> t
      """, expected)
  }

  fun `test right rectangular`() {
    val expected = listOf(LogicalPositionInLine(0, 61),
                          LogicalPositionInLine(0, 70),
                          LogicalPositionInLine(2, 70, PositionInLine.BOTTOM),
                          LogicalPositionInLine(2, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 0, PositionInLine.BOTTOM),
                          LogicalPositionInLine(0, 61, PositionInLine.BOTTOM))
    checkPath("""
      |class DateRange(val start: MyDate, val endInclusive: MyDate) <placeholder>{
      |    operator fun contains(d: MyDate) = d >= start && d <= endInclusive
      |}</placeholder>
    """, expected)
  }

  fun `test left rectangular`() {
    val expected = listOf(LogicalPositionInLine(0, 0),
                          LogicalPositionInLine(0, 11),
                          LogicalPositionInLine(1, 11, PositionInLine.BOTTOM),
                          LogicalPositionInLine(1, 8, PositionInLine.BOTTOM),
                          LogicalPositionInLine(2, 8, PositionInLine.BOTTOM),
                          LogicalPositionInLine(2, 0, PositionInLine.BOTTOM))
    checkPath("""
      |<placeholder>    example
      |test
      |testtets</placeholder> = 2
    """, expected)
  }

  fun `test soft wrap`() {
    val text = """
      |<placeholder>test test test
      |test
      |test</placeholder> = 2
    """
    val placeholders = getPlaceholders(text, true)
    val placeholderShape = getPlaceholderShape(myFixture.editor, placeholders[0].offset, placeholders[0].endOffset)
    val bottomY = myFixture.editor.logicalPositionToXY(LogicalPositionInLine(2, 0)).y
    val maxX = myFixture.editor.contentComponent.x + myFixture.editor.contentComponent.width
    val x1 = LogicalPositionInLine(3, 4).toPoint(myFixture.editor).x
    val expected = listOf(Point(0, 0),
                          Point(maxX, 0),
                          Point(maxX, bottomY),
                          Point(x1, bottomY),
                          Point(x1, bottomY + myFixture.editor.lineHeight),
                          Point(0, bottomY + myFixture.editor.lineHeight))
    checkPointsCyclically(expected, placeholderShape.points)
  }

  fun `test soft wrap last line`() {
    val text = """
      |<placeholder>exam
      |test
      |testr test test = 2</placeholder>
    """
    val placeholders = getPlaceholders(text, true)
    val placeholderShape = getPlaceholderShape(myFixture.editor, placeholders[0].offset, placeholders[0].endOffset)
    val bottomY = myFixture.editor.logicalPositionToXY(LogicalPositionInLine(3, 0)).y
    val maxX = myFixture.editor.contentComponent.x + myFixture.editor.contentComponent.width
    val expected = listOf(Point(0, 0),
                          Point(maxX, myFixture.editor.logicalPositionToXY(LogicalPositionInLine(0, 0)).y),
                          Point(maxX, bottomY),
                          Point(0, bottomY))
    checkPointsCyclically(expected, placeholderShape.points)
  }

  private fun checkRectangular(text: String, start: LogicalPosition, end: LogicalPosition) {
    val expected = listOf(LogicalPositionInLine(start.line, start.column),
                          LogicalPositionInLine(start.line, end.column),
                          LogicalPositionInLine(end.line, end.column, PositionInLine.BOTTOM),
                          LogicalPositionInLine(end.line, start.column, PositionInLine.BOTTOM))
    checkPath(text, expected)
  }

  private fun checkPath(text: String, expected: List<LogicalPositionInLine>, useSoftWrap: Boolean = false) {
    val placeholders = getPlaceholders(text, useSoftWrap)
    val placeholderShape = getPlaceholderShape(myFixture.editor, placeholders[0].offset, placeholders[0].endOffset)
    checkPointsCyclically(expected.map { it.toPoint(myFixture.editor) }, placeholderShape.points)
  }

  private fun checkPointsCyclically(expectedPoints: List<Point>, actual: List<Point>) {
    assertEquals("Wrong number of points\n" + getMessage(expectedPoints, actual), expectedPoints.size, actual.size)
    val shift = expectedPoints.indexOf(actual[0])
    assertTrue(getMessage(expectedPoints, actual), shift >= 0)
    for (i in 0 until expectedPoints.size) {
      assertEquals(getMessage(expectedPoints, actual), expectedPoints[(i + shift) % expectedPoints.size], actual[i])
    }
  }

  private fun getPlaceholders(text: String, useSoftWrap: Boolean = false): List<AnswerPlaceholder> {
    myFixture.configureByText("file.txt", text.trimMargin("|"))
    if (useSoftWrap)
      EditorTestUtil.configureSoftWraps(myFixture.editor, 10)
    val placeholders = CCTestCase.getPlaceholders(myFixture.editor.document, true)
    assertEquals("Wrong number of placeholders", 1, placeholders.size)
    return placeholders
  }

  data class LogicalPositionInLine(private val line: Int,
                                   private val column: Int,
                                   private val position: PositionInLine = PositionInLine.TOP,
                                   private val wrapped: Boolean = false) : LogicalPosition(line, column) {
    fun toPoint(editor: Editor): Point {
      val point = editor.logicalPositionToXY(this)
      var y = point.y
      var x = point.x
      if (position == PositionInLine.BOTTOM) {
        y += editor.lineHeight
      }
      if (wrapped) {
        y += editor.lineHeight
        x = editor.contentComponent.x + editor.contentComponent.width
      }
      return Point(x, y)
    }
  }

  enum class PositionInLine {
    TOP, BOTTOM
  }

  private fun getMessage(expected: List<Point>, actual: List<Point>) = "Actual path: $actual\nExpected path:$expected"
}
