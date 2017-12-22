package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.LogicalPosition
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.editor.NewPlaceholderPainter.toPoint
import org.junit.Test
import java.awt.Point

class PlaceholderPainterTest : EduTestCase() {

  @Test
  fun `test inline placeholder`() {
    checkRectangular("""
    |This is <placeholder>one line </placeholder> placeholder
    """, LogicalPosition(0, 8), LogicalPosition(0, 17))
  }

  @Test
  fun `test one empty line`() {
    checkRectangular("""
    |fun checkSign(number: Int) = <placeholder>
    |    if (number > 0)</placeholder>
    """, LogicalPosition(1, 4), LogicalPosition(1, 19))
  }

  @Test
  fun `test multi line rectangle`() {
    checkRectangular("""
    |def f():
    |<placeholder>    pass
    |    pass
    |    pass</placeholder>
    |
    |f()
    """, LogicalPosition(1, 4), LogicalPosition(3, 8))
  }

  @Test
  fun `test with left margin`(){
    checkRectangular("""
     |t <placeholder>tt
     |  tt</placeholder>
    """, LogicalPosition(0, 2), LogicalPosition(1, 4))
  }

  @Test
  fun `test with right margin`() {
    checkRectangular("""
     |<placeholder>tt
     |tt</placeholder> t
    """, LogicalPosition(0, 0), LogicalPosition(1, 2))
  }



  @Test
  fun `test non rectangular`() {
    val expected = listOf(NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 2), NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 4),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 4, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 2, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(1, 2, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(1, 0, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 0, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 2, NewPlaceholderPainter.PositionInLine.BOTTOM))
    checkPath("""
      |t <placeholder>tt
      |tt</placeholder> t
      """, expected)
  }

  @Test
  fun `test right rectangular`() {
    val expected = listOf(NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 61), NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 70),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(2, 70, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(2, 0, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 0, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 61, NewPlaceholderPainter.PositionInLine.BOTTOM))
    checkPath("""
      |class DateRange(val start: MyDate, val endInclusive: MyDate) <placeholder>{
      |    operator fun contains(d: MyDate) = d >= start && d <= endInclusive
      |}</placeholder>
    """, expected)
  }

  @Test
  fun `test left rectangular`() {
    val expected = listOf(NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 0),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(0, 11),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(1, 11, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(1, 8, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(2, 8, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(2, 0, NewPlaceholderPainter.PositionInLine.BOTTOM))
    checkPath("""
      |<placeholder>    example
      |test
      |testtets</placeholder> = 2
    """, expected)
  }


  @Test
  fun `test last line longer`() {
    checkRectangular("""
      |<placeholder>test
      |testtest
      |testtesttest</placeholder>
    """, LogicalPosition(0, 0), LogicalPosition(2, 12))
  }


  @Test
  fun `test first line longer`() {
    checkRectangular("""
      |<placeholder>testtesttest
      | testtest
      | test</placeholder>
    """, LogicalPosition(0, 0), LogicalPosition(2, 12))
  }

  private fun checkRectangular(text: String, start: LogicalPosition, end: LogicalPosition) {
    val expected = listOf(NewPlaceholderPainter.LogicalPositionWithLinePlacement(start.line, start.column),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(start.line, end.column),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(end.line, end.column, NewPlaceholderPainter.PositionInLine.BOTTOM),
                          NewPlaceholderPainter.LogicalPositionWithLinePlacement(end.line, start.column, NewPlaceholderPainter.PositionInLine.BOTTOM))
    checkPath(text, expected)
  }

  private fun checkPath(text: String, expected: List<NewPlaceholderPainter.LogicalPositionWithLinePlacement>) {
    val placeholders = getPlaceholders(text)
    val path = NewPlaceholderPainter.getPath(myFixture.editor, placeholders[0].offset, placeholders[0].endOffset)
    checkCyclically(expected, path)
  }

  private fun checkCyclically(expected: List<NewPlaceholderPainter.LogicalPositionWithLinePlacement>, actual: List<Point>) {
    checkPointsCyclically(expected.map { toPoint(myFixture.editor, it) }, actual)
  }

  //TODO: check in both directions?
  private fun checkPointsCyclically(expectedPoints: List<Point>, actual: List<Point>) {
    assertEquals("Wrong number of points\n" + getMessage(expectedPoints, actual), expectedPoints.size, actual.size)
    val shift = expectedPoints.indexOf(actual[0])
    assertTrue(getMessage(expectedPoints, actual), shift >= 0)
    for (i in 0 until expectedPoints.size) {
      assertEquals(getMessage(expectedPoints, actual), expectedPoints[(i + shift) % expectedPoints.size], actual[i])
    }
  }

  private fun getPlaceholders(text: String): List<AnswerPlaceholder> {
    myFixture.configureByText("file.txt", text.trimMargin("|"))
    val placeholders = CCTestCase.getPlaceholders(myFixture.editor.document, true)
    assertEquals("Wrong number of placeholders", 1, placeholders.size)
    return placeholders
  }

  private fun getMessage(expected: List<Point>, actual: List<Point>) = "Actual path: $actual\nExpected path:$expected"
}
