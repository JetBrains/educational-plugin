package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import java.awt.Point


sealed class PlaceholderShape {
  val points = ArrayList<Point>()

  protected fun getRectangularLeftBorder(editor: Editor,
                                         bottom: LogicalPosition,
                                         left: LogicalPosition,
                                         right: LogicalPosition) = listOf(
    NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(bottom.line, bottom.column,
                                                                                                 NewPlaceholderPainter.PositionInLine.BOTTOM)),
    editor.logicalPositionToXY(left), editor.logicalPositionToXY(right))

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


  class Rectangular(editor: Editor, start: LogicalPosition, end: LogicalPosition) : PlaceholderShape() {
    init {
      val startPoint = NewPlaceholderPainter.toPoint(editor,
                                                     NewPlaceholderPainter.LogicalPositionWithLinePlacement(start.line, start.column))
      val endPoint = NewPlaceholderPainter.toPoint(editor, NewPlaceholderPainter.LogicalPositionWithLinePlacement(end.line, end.column,
                                                                                                                  NewPlaceholderPainter.PositionInLine.BOTTOM))
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
