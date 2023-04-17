package com.jetbrains.edu.learning.placeholder

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import java.awt.Point
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.GeneralPath
import kotlin.math.roundToInt


sealed class PlaceholderShape {
  val points = ArrayList<Point>()

  fun getShape(): Shape {
    if (points.size == 2) {
      return Rectangle(points[0].x, points[0].y, 1, points[1].y - points[0].y)
    }
    val generalPath = GeneralPath()
    generalPath.moveTo(points[0].x.toDouble(), points[0].y.toDouble())
    for (i in 1 until points.size) {
      generalPath.lineTo(points[i].x.toDouble(), points[i].y.toDouble())
    }
    generalPath.closePath()
    return generalPath
  }

  // Empty placeholder
  class Line(editor: Editor, point1: Point, point2: Point) : PlaceholderShape() {
    init {
      points.addAll(listOf(visiblePoint(editor, point1),
                           visiblePoint(editor, point2.bottom(editor))))
    }
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
  class Complex(editor: Editor, point1: Point, point3: Point, point5: Point, point7: Point) : PlaceholderShape() {
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
    val visibleX = when {
      point.x < visibleRect.x -> visibleRect.x
      point.x > visibleRect.x + visibleRect.width -> visibleRect.x + visibleRect.width
      else -> point.x
    }

    // We have to take into account stroke width to avoid weird rendering artifacts
    val minVisibleY = visibleRect.y + HALF_STROKE_WIDTH
    val maxVisibleY = visibleRect.y + visibleRect.height - HALF_STROKE_WIDTH
    val visibleY = when {
      point.y < minVisibleY -> minVisibleY
      point.y > maxVisibleY -> maxVisibleY
      else -> point.y
    }

    return Point(visibleX, visibleY)
  }

  companion object {
    private val HALF_STROKE_WIDTH: Int = (PlaceholderPainter.STROKE_WIDTH / 2).roundToInt()
  }
}

fun getPlaceholderShape(editor: Editor, startOffset: Int, endOffset: Int): PlaceholderShape {
  val document = editor.document
  val startLine = document.getLineNumber(startOffset)
  val endLine = document.getLineNumber(endOffset)
  val leftTop = editor.offsetToXY(startOffset)
  val rightBottom = editor.offsetToXY(endOffset)

  if (startOffset == endOffset) {
    return PlaceholderShape.Line(editor, leftTop, rightBottom)
  }
  val boundaries = (startLine..endLine).map {
    LineBoundary(document.getLineStartOffset(it), document.getLineEndOffset(it))
  }

  var isLeftRectangular = leftTop.y == rightBottom.y
  var isRightRectangular = leftTop.y == rightBottom.y
  var leftMinBoundary = LineBoundary(startOffset, endOffset)
  var rightMaxBoundary = LineBoundary(startOffset, endOffset)

  if (startLine != endLine) {
    leftMinBoundary = boundaries.subList(1, boundaries.size).minByOrNull { it.getVisualBoundaries(editor).first } ?: leftMinBoundary
    val endBoundary = if (endOffset == document.getLineEndOffset(endLine)) boundaries.size else boundaries.size - 1
    rightMaxBoundary = boundaries.subList(0, endBoundary).maxByOrNull { it.getVisualBoundaries(editor).second } ?: rightMaxBoundary

    isLeftRectangular = editor.columnByOffset(leftMinBoundary.startOffset) >= editor.columnByOffset(startOffset)
    isRightRectangular = editor.columnByOffset(rightMaxBoundary.endOffset) <= editor.columnByOffset(endOffset) ||
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

data class LineBoundary(val startOffset: Int, val endOffset: Int)

private fun LineBoundary.getVisualBoundaries(editor: Editor): Pair<Int, Int> {
  val leftXY = editor.offsetToXY(startOffset)
  val rightXY = editor.offsetToXY(endOffset)

  if (leftXY.y != rightXY.y) {
    return Pair(0, editor.contentComponent.x + editor.contentComponent.width)
  }
  return Pair(leftXY.x, rightXY.x)
}

private fun Editor.columnByOffset(offset: Int): Int = offsetToLogicalPosition(offset).column
