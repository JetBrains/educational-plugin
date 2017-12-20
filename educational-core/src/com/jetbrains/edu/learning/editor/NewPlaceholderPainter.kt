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


    if (lineX == lineY) {
      //always a rectangular
      //TODO: extract a function that makes a rectangular path?
      points.add(Point(xPoint.x, xPoint.y))
      points.add(Point(yPoint.x, xPoint.y))
      points.add(Point(yPoint.x, yPoint.y + lineHeight))
      points.add(Point(xPoint.x, yPoint.y + lineHeight))
      return points
    }

    val boundaries = (lineX..lineY).map { getBoundaries(document, it) }

    //TODO: why nullable?
    val left = boundaries.subList(1, boundaries.size).minBy(LineBoundary::left)!!
    val right = boundaries.subList(0, boundaries.size - 1).maxBy({ it.getVisualBoundaries(editor).second })!!

    val isLeftRectangular = boundaries[0].left >= editor.offsetToLogicalPosition(x).column
    val isRightRectangular = boundaries[boundaries.size - 1].right <= editor.offsetToLogicalPosition(y).column


    val rightPointX = maxOf(editor.logicalPositionToXY(LogicalPosition(right.line, right.right)).x, yPoint.x)
    val leftPointX = minOf(editor.logicalPositionToXY(LogicalPosition(left.line, left.left)).x, xPoint.x)

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
}
