package com.jetbrains.edu.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import java.awt.Point
import java.awt.Shape
import java.awt.geom.GeneralPath


sealed class PlaceholderShape {
  val points = ArrayList<Point>()

  fun getShape(): Shape {
    val generalPath = GeneralPath()
    generalPath.moveTo(points[0].x.toDouble(), points[0].y.toDouble())
    for (i in 1 until points.size) {
      generalPath.lineTo(points[i].x.toDouble(), points[i].y.toDouble())
    }
    generalPath.closePath()
    return generalPath
  }

  // 4 points clock-wise from left top point
  // 1---------------2
  // ----------------
  // 4---------------3
  class Rectangular(editor: Editor, point1: Point, point3: Point) : PlaceholderShape() {
    init {
      val point2 = Point(point3.x, point1.y)
      val point4 = Point(point1.x, point3.y)
      points.addAll(listOf(visiblePoint(editor, point1),
                           visiblePoint(editor, point2),
                           visiblePoint(editor, point3.bottom(editor)),
                           visiblePoint(editor, point4.bottom(editor))))
    }
  }

  // 6 points clock-wise from left top point
  // 1---------------2
  // ----------------
  // ---------4------3
  // 6--------5
  class LeftRectangular(editor: Editor, point1: Point, point3: Point, point5: Point) : PlaceholderShape() {
    init {
      val point2 = Point(point3.x, point1.y)
      val point4 = Point(point5.x, point3.y)
      val point6 = Point(point1.x, point5.y)
      points.addAll(listOf(visiblePoint(editor, point1),
                           visiblePoint(editor, point2),
                           visiblePoint(editor, point3),
                           visiblePoint(editor, point4),
                           visiblePoint(editor, point5.bottom(editor)),
                           visiblePoint(editor, point6.bottom(editor))
                           ))
    }
  }

  // 6 points clock-wise from left top point
  //     1-----------2
  // 5---6-----------
  // ----------------
  // 4---------------3
  class RightRectangular(editor: Editor, point1: Point, point3: Point, point5: Point) : PlaceholderShape() {
    init {
      val point2 = Point(point3.x, point1.y)
      val point4 = Point(point5.x, point3.y)
      val point6 = Point(point1.x, point5.y)
      points.addAll(listOf(visiblePoint(editor, point1),
                           visiblePoint(editor, point2),
                           visiblePoint(editor, point3.bottom(editor)),
                           visiblePoint(editor, point4.bottom(editor)),
                           visiblePoint(editor, point5),
                           visiblePoint(editor, point6)
      ))
    }
  }

  // 8 points clock-wise from left top point
  //     1-----------2
  // 7---8-----------
  // ----------------
  // ------------4---3
  // 6-----------5
  class Complex(editor: Editor, point1: Point, point3: Point, point5: Point, point7: Point): PlaceholderShape() {
    init {
      val point2 = Point(point3.x, point1.y)
      val point4 = Point(point5.x, point3.y)
      val point6 = Point(point7.x, point5.y)
      val point8 = Point(point1.x, point7.y)

      points.addAll(listOf(visiblePoint(editor, point1),
                           visiblePoint(editor, point2),
                           visiblePoint(editor, point3),
                           visiblePoint(editor, point4),
                           visiblePoint(editor, point5.bottom(editor)),
                           visiblePoint(editor, point6.bottom(editor)),
                           visiblePoint(editor, point7),
                           visiblePoint(editor, point8)
      ))
    }
  }

  fun Point.bottom(editor: Editor): Point {
    return Point(x, y + editor.lineHeight)
  }

  fun visiblePoint(editor: Editor, point: Point): Point {
    val contentComponent = editor.contentComponent
    val visibleRect = contentComponent.visibleRect
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return point
    }
    if (point.x > visibleRect.x + visibleRect.width) {
      return Point(visibleRect.x + visibleRect.width, point.y)
    }
    if (point.x < visibleRect.x)
      return Point(visibleRect.x, point.y)
    return point
  }
}

fun getPlaceholderShape(editor: Editor, startOffset: Int, endOffset: Int): PlaceholderShape {
  val document = editor.document
  val startLine = document.getLineNumber(startOffset)
  val endLine = document.getLineNumber(endOffset)
  val leftTop = editor.offsetToXY(startOffset)
  val rightBottom = editor.offsetToXY(endOffset)

  val boundaries = (startLine..endLine).map {
    LineBoundary(it, 0, document.getLineEndOffset(it) - document.getLineStartOffset(it)) }

  var isLeftRectangular = leftTop.y == rightBottom.y
  var isRightRectangular = leftTop.y == rightBottom.y
  val lineStart = document.getLineStartOffset(startLine)
  var leftMinBoundary = LineBoundary(startLine, startOffset - lineStart, endOffset - lineStart)
  var rightMaxBoundary = LineBoundary(startLine, startOffset - lineStart, endOffset - lineStart)

  if (startLine != endLine) {
    leftMinBoundary = boundaries.subList(1, boundaries.size).minBy{ it.getVisualBoundaries(editor).first } ?: leftMinBoundary
    rightMaxBoundary = boundaries.subList(0, boundaries.size - 1).maxBy{ it.getVisualBoundaries(editor).second } ?: rightMaxBoundary

    isLeftRectangular = leftMinBoundary.left >= editor.offsetToLogicalPosition(startOffset).column
    isRightRectangular = rightMaxBoundary.right <= editor.offsetToLogicalPosition(endOffset).column ||
      document.getLineEndOffset(endLine) == endOffset
  }

  val rightPointX = maxOf(rightMaxBoundary.getVisualBoundaries(editor).second, rightBottom.x)
  val leftPointX = minOf(leftMinBoundary.getVisualBoundaries(editor).first, leftTop.x)

  if (isLeftRectangular && isRightRectangular) {
    return PlaceholderShape.Rectangular(editor, Point(leftPointX, leftTop.y), Point(rightPointX, rightBottom.y))
  }

  val secondLineY = editor.offsetToXY(document.getLineStartOffset(startLine)).y + editor.lineHeight
  val penultimateLineY = editor.offsetToXY(document.getLineEndOffset(endLine)).y

  if (isLeftRectangular) {
    val point3 = Point(rightPointX, penultimateLineY)
    val point5 = editor.offsetToXY(endOffset)
    return PlaceholderShape.LeftRectangular(editor, leftTop, point3, point5)
  }

  if (isRightRectangular) {
    val point3 = Point(rightPointX, penultimateLineY)
    val point5 = Point(leftPointX, secondLineY)
    return PlaceholderShape.RightRectangular(editor, leftTop, point3, point5)
  }

  val point3 = Point(rightPointX, penultimateLineY)
  val point5 = editor.offsetToXY(endOffset)
  val point7 = Point(leftPointX, secondLineY)
  return PlaceholderShape.Complex(editor, leftTop, point3, point5, point7)
}

data class LineBoundary(val line: Int, val left: Int, val right: Int)

private fun LineBoundary.getVisualBoundaries(editor: Editor): Pair<Int, Int> {
  val leftPosition = LogicalPosition(this.line, this.left)
  val rightPosition = LogicalPosition(this.line, this.right)

  val leftXY = editor.logicalPositionToXY(leftPosition)
  val rightXY = editor.logicalPositionToXY(rightPosition)

  if (leftXY.y != rightXY.y) {
    return Pair(0, editor.contentComponent.x + editor.contentComponent.width)
  }
  return Pair(leftXY.x, rightXY.x)
}

