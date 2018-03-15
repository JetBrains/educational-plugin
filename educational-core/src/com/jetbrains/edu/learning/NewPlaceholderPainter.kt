package com.jetbrains.edu.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.AbstractPainter
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Graphics2D
import java.awt.Shape


object NewPlaceholderPainter {
  val placeholderPainters = HashMap<AnswerPlaceholder, AbstractPainter>()

  fun paintPlaceholder(editor: Editor, placeholder: AnswerPlaceholder) {
    val painter: AbstractPainter = object : AbstractPainter() {

      override fun needsRepaint() = !editor.isDisposed

      override fun executePaint(component: Component?, g: Graphics2D) {
        g.color = placeholder.color
        g.stroke = BasicStroke(JBUI.scale(2f))
        val shape = getPlaceholderShape(editor, placeholder.offset, placeholder.endOffset).getShape()
        if (!isVisible(shape, editor)) return
        g.draw(shape)
      }
    }
    placeholderPainters[placeholder] = painter
    IdeGlassPaneUtil.installPainter(editor.contentComponent, painter, editor.project)
  }

  private fun isVisible(shape: Shape, editor: Editor) : Boolean {
    return editor.contentComponent.visibleRect.contains(shape.bounds)
  }

  fun removePainter(editor: Editor, placeholder: AnswerPlaceholder) {
    if (placeholderPainters.containsKey(placeholder)) {
      val painter = placeholderPainters.remove(placeholder)
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        IdeGlassPaneUtil.find(editor.contentComponent)?.removePainter(painter)
      }
    }
  }


}
