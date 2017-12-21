package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.ui.AbstractPainter
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.GeneralPath


object NewPlaceholderPainter {
  fun paintPlaceholder(editor: Editor, placeholder: AnswerPlaceholder) {
//    val highlighter = editor.markupModel.addRangeHighlighter(placeholder.offset, placeholder.endOffset, HighlighterLayer.LAST, null,
//                                                             HighlighterTargetArea.EXACT_RANGE)
//    highlighter.customRenderer = EduPlaceholderRenderer(placeholder)
    IdeGlassPaneUtil.installPainter(editor.contentComponent, object: AbstractPainter() {

      override fun needsRepaint() = true

      override fun executePaint(component: Component?, g: Graphics2D) {
        g.color = placeholder.color
        val path = getPath(editor, placeholder.offset, placeholder.endOffset)
        val generalPath = GeneralPath()
        generalPath.moveTo(path[0].x.toDouble(), path[0].y.toDouble())
        for (i in 1 until path.size) {
          generalPath.lineTo(path[i].x.toDouble(), path[i].y.toDouble())
        }
        generalPath.closePath()
        g.draw(generalPath)
      }
    }, editor.project)
  }


  private class EduPlaceholderRenderer(val placeholder: AnswerPlaceholder) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      g.color = placeholder.color
      val path = getPath(editor, placeholder.offset, placeholder.endOffset)
      val generalPath = GeneralPath()
      generalPath.moveTo(path[0].x.toDouble(), path[0].y.toDouble())
      for (i in 1 until path.size) {
        generalPath.lineTo(path[i].x.toDouble(), path[i].y.toDouble())
      }
      generalPath.closePath()
      (g as Graphics2D).draw(generalPath)
    }
  }


  //TODO: make this method return placeholder shape
  fun getPath(editor: Editor, start: Int, end: Int): List<Point> {
    val x = getFirstNonWhitespace(editor.document, start..end)
    val y = getFirstNonWhitespace(editor.document, end..start)
    //TODO: kotlin api for that?
    val points = ContainerUtil.newArrayList<Point>()
    val document = editor.document
    val lineX = document.getLineNumber(x)
    val lineY = document.getLineNumber(y)
    val xPoint = editor.offsetToXY(x)
    val yPoint = editor.offsetToXY(y)
    val lineHeight = getLineHeight(editor)


    //TODO: do we really need this are special case
    if (lineX == lineY) {
      return PlaceholderShape.Rectangular(editor, editor.offsetToLogicalPosition(x), editor.offsetToLogicalPosition(y)).points
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
      val startPosition = editor.xyToLogicalPosition(Point(leftPointX, xPoint.y))
      val endPosition = editor.xyToLogicalPosition(Point(rightPointX, yPoint.y))
      return PlaceholderShape.Rectangular(editor, startPosition, endPosition).points
    }

    val rightBottom = LogicalPosition(lineY, editor.xyToLogicalPosition(Point(rightPointX, yPoint.y)).column)
    val rightTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(rightPointX, xPoint.y)).column)
    val leftBottom = LogicalPosition(lineY, editor.xyToLogicalPosition(Point(leftPointX, xPoint.y)).column)
    if (isLeftRectangular) {
      val leftTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(leftPointX, xPoint.y)).column)
      return PlaceholderShape.LeftRectangular(editor, leftBottom, leftTop, rightTop, rightBottom).points
    }

    if (isRightRectangular) {
      val leftTop = LogicalPosition(lineX, editor.xyToLogicalPosition(Point(xPoint.x, xPoint.y)).column)
      return PlaceholderShape.RightRectangular(editor, leftBottom, leftTop, rightTop, rightBottom).points
    }

    //add left and upper borders
    if (!isLeftRectangular) {
      val nextLineY = xPoint.y + lineHeight
      points.add(Point(leftPointX, yPoint.y + lineHeight))
      if (leftPointX == xPoint.x) {
        points.add(Point(xPoint.x, xPoint.y))
      }
      else {
        points.add(Point(leftPointX, nextLineY))
        points.add(Point(xPoint.x, nextLineY))
        points.add(Point(xPoint.x, xPoint.y))
      }

      points.add(Point(rightPointX, xPoint.y))
    }
    else {
      points.add(Point(leftPointX, yPoint.y + lineHeight))
      points.add(Point(leftPointX, xPoint.y))
      points.add(Point(rightPointX, xPoint.y))
    }

    val yLineBottom = yPoint.y + lineHeight
    //draw right and bottom borders
    if (!isRightRectangular) {
      val prevLineY = yLineBottom - lineHeight
      if (rightPointX == yPoint.x) {
        points.add(Point(yPoint.x, yLineBottom))
      }
      else {
        points.add(Point(rightPointX, prevLineY))
        points.add(Point(yPoint.x, prevLineY))
        points.add(Point(yPoint.x, yLineBottom))
      }
    }
    else {
      points.add(Point(rightPointX, yLineBottom))
    }

    return points
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

  sealed class PlaceholderShape {
    val points = ArrayList<Point>()

    protected fun getRectangularLeftBorder(editor: Editor,
                                           bottom: LogicalPosition,
                                           left: LogicalPosition,
                                           right: LogicalPosition) = listOf(
      toPoint(editor, LogicalPositionWithLinePlacement(bottom.line, bottom.column, PositionInLine.BOTTOM)),
      editor.logicalPositionToXY(left), editor.logicalPositionToXY(right))

    protected fun getRectangularRightBorder(editor: Editor, rightBottom: LogicalPosition): List<Point> = listOf(
      toPoint(editor, LogicalPositionWithLinePlacement(rightBottom.line, rightBottom.column, PositionInLine.BOTTOM)))


    protected fun getComplexRightBorder(editor: Editor, rightTop: LogicalPosition, rightBottom: LogicalPosition): List<Point> {
      val rightTopPoint = editor.logicalPositionToXY(rightTop)
      val rightBottomPoint = toPoint(editor, LogicalPositionWithLinePlacement(rightBottom.line, rightBottom.column, PositionInLine.BOTTOM))
      if (rightTopPoint.x == rightBottomPoint.x) {
        return listOf(Point(rightBottomPoint))
      }
      val topPoint = toPoint(editor, LogicalPositionWithLinePlacement(rightBottom.line - 1, rightTop.column, PositionInLine.BOTTOM))
      return listOf(topPoint, Point(rightBottomPoint.x, topPoint.y), rightBottomPoint)
    }

    protected fun getComplexLeftBorder(editor: Editor,
                                       leftBottom: LogicalPosition,
                                       leftTop: LogicalPosition,
                                       rightTop: LogicalPosition): List<Point> {
      val leftBottomPoint = toPoint(editor, LogicalPositionWithLinePlacement(leftBottom.line, leftBottom.column, PositionInLine.BOTTOM))
      val leftTopPoint = editor.logicalPositionToXY(leftTop)
      val shape = mutableListOf(leftBottomPoint)
      if (leftBottomPoint.x == leftTopPoint.x) {
        shape.add(leftTopPoint)
      }
      else {
        shape.add(toPoint(editor, LogicalPositionWithLinePlacement(leftTop.line, leftBottom.column, PositionInLine.BOTTOM)))
        shape.add(toPoint(editor, LogicalPositionWithLinePlacement(leftTop.line, leftTop.column, PositionInLine.BOTTOM)))
        shape.add(Point(leftTopPoint))
      }
      shape.add(editor.logicalPositionToXY(rightTop))
      return shape
    }


    class Rectangular(editor: Editor, start: LogicalPosition, end: LogicalPosition) : PlaceholderShape() {
      init {
        val startPoint = toPoint(editor, LogicalPositionWithLinePlacement(start.line, start.column))
        val endPoint = toPoint(editor, LogicalPositionWithLinePlacement(end.line, end.column, PositionInLine.BOTTOM))
        points.addAll(listOf(startPoint, Point(endPoint.x, startPoint.y), endPoint, Point(startPoint.x, endPoint.y)))
      }
    }

    class LeftRectangular(editor: Editor,
                          leftBottom: LogicalPosition,
                          leftTop: LogicalPosition,
                          rightTop: LogicalPosition,
                          rightBottom: LogicalPosition) : PlaceholderShape() {
      init {
        points.addAll(getRectangularLeftBorder(editor, leftBottom, leftTop, rightTop))
        points.addAll(getComplexRightBorder(editor, rightTop, rightBottom))
      }
    }

    class RightRectangular(editor: Editor,
                           leftBottom: LogicalPosition,
                           leftTop: LogicalPosition,
                           rightTop: LogicalPosition,
                           rightBottom: LogicalPosition) : PlaceholderShape() {
      init {
        points.addAll(getRectangularRightBorder(editor, rightBottom))
        points.addAll(getComplexLeftBorder(editor, leftBottom, leftTop, rightTop))
      }
    }

//    object Complex: PlaceholderShape() {
//
//    }
  }

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
