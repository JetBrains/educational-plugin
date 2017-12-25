package com.jetbrains.edu.learning.editor

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


  protected fun getRectangularLeftBorder(editor: Editor,
                                         leftBottom: LogicalPosition,
                                         leftTop: LogicalPosition,
                                         rightTop: LogicalPosition) = listOf(
    NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(leftBottom.line, leftBottom.column,
                                                                                                 NewPlaceholderPainter.PositionInLine.BOTTOM)),
    editor.logicalPositionToXY(leftTop), editor.logicalPositionToXY(rightTop))

  protected fun getRectangularRightBorder(editor: Editor, rightBottom: LogicalPosition): List<Point> = listOf(
    NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(rightBottom.line, rightBottom.column,
                                                                                                 NewPlaceholderPainter.PositionInLine.BOTTOM)))


  protected fun getComplexRightBorder(editor: Editor, rightTop: LogicalPosition, rightBottom: LogicalPosition): List<Point> {
    val rightTopPoint = editor.logicalPositionToXY(rightTop)
    val rightBottomPoint = NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(rightBottom.line,
                                                                                                                        rightBottom.column,
                                                                                                                        NewPlaceholderPainter.PositionInLine.BOTTOM))
    if (rightTopPoint.x == rightBottomPoint.x) {
      return listOf(Point(rightBottomPoint))
    }
    val topPoint = NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(rightBottom.line - 1,
                                                                                                                rightTop.column,
                                                                                                                NewPlaceholderPainter.PositionInLine.BOTTOM))
    return listOf(topPoint, Point(rightBottomPoint.x, topPoint.y), rightBottomPoint)
  }

  protected fun getComplexLeftBorder(editor: Editor,
                                     leftBottom: LogicalPosition,
                                     leftTop: LogicalPosition,
                                     rightTop: LogicalPosition): List<Point> {
    val leftBottomPoint = NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(leftBottom.line,
                                                                                                                       leftBottom.column,
                                                                                                                       NewPlaceholderPainter.PositionInLine.BOTTOM))
    val leftTopPoint = editor.logicalPositionToXY(leftTop)
    val shape = mutableListOf(leftBottomPoint)
    if (leftBottomPoint.x == leftTopPoint.x) {
      shape.add(leftTopPoint)
    }
    else {
      shape.add(NewPlaceholderPainter.toPoint(editor,
                                              NewPlaceholderPainter.LogicalPositionWithLinePlacement(leftTop.line, leftBottom.column,
                                                                                                     NewPlaceholderPainter.PositionInLine.BOTTOM)))
      shape.add(NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(leftTop.line, leftTop.column,
                                                                                                             NewPlaceholderPainter.PositionInLine.BOTTOM)))
      shape.add(Point(leftTopPoint))
    }
    shape.add(editor.logicalPositionToXY(rightTop))
    return shape
  }


  class Rectangular(editor: Editor, leftTop: LogicalPosition, rightBottom: LogicalPosition) : PlaceholderShape() {
    init {
      points.addAll(getRectangularLeftBorder(editor, LogicalPosition(rightBottom.line, leftTop.column), leftTop, LogicalPosition(
        leftTop.line, rightBottom.column)))
      points.addAll(getRectangularRightBorder(editor, rightBottom))
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

  class Complex(editor: Editor,
                leftBottom: LogicalPosition,
                leftTop: LogicalPosition,
                rightTop: LogicalPosition,
                rightBottom: LogicalPosition): PlaceholderShape() {
    init {
      points.addAll(getComplexLeftBorder(editor, leftBottom, leftTop, rightTop))
      points.addAll(getComplexRightBorder(editor, rightTop, rightBottom))
    }
  }
}
