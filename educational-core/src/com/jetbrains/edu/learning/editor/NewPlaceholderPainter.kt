package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints


object NewPlaceholderPainter {
  fun paintPlaceholder(editor: Editor, placeholder: AnswerPlaceholder) {
    val highlighter = editor.markupModel.addRangeHighlighter(placeholder.offset, placeholder.endOffset, HighlighterLayer.LAST, null,
                                                             HighlighterTargetArea.EXACT_RANGE)
    highlighter.customRenderer = EduPlaceholderRenderer(placeholder)
  }


  private class EduPlaceholderRenderer(val placeholder: AnswerPlaceholder) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      g.color = placeholder.color
      val old = (g as Graphics2D).getRenderingHint(RenderingHints.KEY_ANTIALIASING)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.draw(getPlaceholderShape(editor, placeholder.offset, placeholder.endOffset).getShape())
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old)
    }
  }


  fun getPlaceholderShape(editor: Editor, start: Int, end: Int): PlaceholderShape {
    val x = getFirstNonWhitespace(editor.document, start..end)
    val y = getFirstNonWhitespace(editor.document, end..start)
    val document = editor.document
    val lineX = document.getLineNumber(x)
    val lineY = document.getLineNumber(y)
    val xPoint = editor.offsetToXY(x)
    val yPoint = editor.offsetToXY(y)


    //TODO: do we really need this are special case
    if (lineX == lineY) {
      return PlaceholderShape.Rectangular(editor, editor.offsetToLogicalPosition(x), editor.offsetToLogicalPosition(y))
    }

    val boundaries = (lineX..lineY).map { getBoundaries(document, it) }

    //TODO: why nullable?
    val left = boundaries.subList(1, boundaries.size).minBy(LineBoundary::left)!!
    val right = boundaries.subList(0, boundaries.size - 1).maxBy({ it.getVisualBoundaries(editor).second })!!

    val isLeftRectangular = boundaries[0].left >= editor.offsetToLogicalPosition(x).column
    val isRightRectangular = boundaries[boundaries.size - 1].right <= editor.offsetToLogicalPosition(y).column

    val rightPointX = maxOf(editor.logicalPositionToXY(LogicalPosition(right.line, right.right)).x, yPoint.x)
    val leftPointX = minOf(editor.logicalPositionToXY(LogicalPosition(left.line, left.left)).x, xPoint.x)


    if (isLeftRectangular && isRightRectangular) {
      val leftTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(leftPointX, xPoint.y)).column)
      val rightBottom = LogicalPosition(lineY, editor.xyToLogicalPosition(Point(rightPointX, yPoint.y)).column)
      return PlaceholderShape.Rectangular(editor, leftTop, rightBottom)
    }

    val rightTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(rightPointX, xPoint.y)).column)
    val leftBottom = LogicalPosition(lineY, editor.xyToLogicalPosition(Point(leftPointX, xPoint.y)).column)
    if (isLeftRectangular) {
      val leftTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(leftPointX, xPoint.y)).column)
      val rightBottom = LogicalPosition(lineY, editor.xyToLogicalPosition(Point(yPoint.x, yPoint.y)).column)
      return PlaceholderShape.LeftRectangular(editor, leftBottom, leftTop, rightTop, rightBottom)
    }

    if (isRightRectangular) {
      val leftTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(xPoint.x, xPoint.y)).column)
      val rightBottom = LogicalPosition(lineY, editor.xyToLogicalPosition(Point(rightPointX, yPoint.y)).column)
      return PlaceholderShape.RightRectangular(editor, leftBottom, leftTop, rightTop, rightBottom)
    }

    val leftTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(xPoint.x, xPoint.y)).column)
    val rightBottom = LogicalPosition(lineY, editor.xyToLogicalPosition(Point(yPoint.x, yPoint.y)).column)
    return PlaceholderShape.Complex(editor, leftBottom, leftTop, rightTop, rightBottom)
  }

  private fun getLineHeight(editor: Editor) = editor.lineHeight - 1


  data class LineBoundary(val line: Int, val left: Int, val right: Int)

  //TODO: handle empty lines correctly?
  private fun getBoundaries(document: Document, line: Int): LineBoundary {
    val start = document.getLineStartOffset(line)
    val end = document.getLineEndOffset(line)
    val left = getFirstNonWhitespace(document, start..end) - start
    val right = getFirstNonWhitespace(document, end..left) - start
    return LineBoundary(line, left, right)
  }

  private fun LineBoundary.getVisualBoundaries(editor: Editor): Pair<Int, Int> {
    val leftPosition = LogicalPosition(this.line, this.left)
    val left = editor.logicalPositionToXY(leftPosition).x
    val rightPosition = LogicalPosition(this.line, this.right)
    val right = editor.logicalPositionToXY(rightPosition).x +
                editor.inlayModel.getInlineElementsInRange(leftPosition.toOffset(editor),
                                                           rightPosition.toOffset(editor)).sumBy { it.widthInPixels }
    return Pair(left, right)
  }

  private fun LogicalPosition.toOffset(editor: Editor) = editor.logicalPositionToOffset(this)


  private fun getFirstNonWhitespace(document: Document, range: IntRange): Int {
    val text = document.immutableCharSequence
    for (offset in range) {
      val char = text[offset]
      if (char.isDrawableSymbol()) {
        return offset
      }
    }
    return range.start
  }

  private fun Char.isDrawableSymbol() = this != ' ' && this != '\t' && this != '\n'

  enum class PositionInLine {
    TOP, BOTTOM
  }

  data class LogicalPositionWithLinePlacement(val line: Int, val column: Int, val position: PositionInLine = PositionInLine.TOP)

  fun toPoint(editor: Editor, logicalPositionWithLinePlacement: LogicalPositionWithLinePlacement): Point {
    val point = editor.logicalPositionToXY(LogicalPosition(logicalPositionWithLinePlacement.line, logicalPositionWithLinePlacement.column))
    if (logicalPositionWithLinePlacement.position == PositionInLine.TOP) {
      return point
    }
    return Point(point.x, point.y + getLineHeight(editor))
  }

}
