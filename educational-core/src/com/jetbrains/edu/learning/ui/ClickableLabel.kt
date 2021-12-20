package com.jetbrains.edu.learning.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class ClickableLabel(text: String, private val onClick: () -> Unit) : JBLabel(text) {
  init {
    alignmentX = Component.LEFT_ALIGNMENT
    border = JBUI.Borders.emptyLeft(5)
    val txt = this.text
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        onClick.invoke()
      }

      override fun mouseEntered(e: MouseEvent?) {
        setText("<html><u>$txt</u></html>")
      }

      override fun mouseExited(e: MouseEvent?) {
        setText(txt)
      }
    })
  }
}