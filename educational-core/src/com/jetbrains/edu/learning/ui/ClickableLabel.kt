package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class ClickableLabel(
  @Suppress("UnstableApiUsage") @NlsContexts.Label val initialText: String,
  private val onClick: () -> Unit
) : JBLabel(initialText) {

  init {
    alignmentX = Component.LEFT_ALIGNMENT
    border = JBUI.Borders.emptyLeft(5)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        onClick.invoke()
      }

      override fun mouseEntered(e: MouseEvent?) {
        setText("<html><u>$initialText</u></html>")
      }

      override fun mouseExited(e: MouseEvent?) {
        setText(initialText)
      }
    })
  }
}