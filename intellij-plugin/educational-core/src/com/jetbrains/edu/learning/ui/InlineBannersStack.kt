package com.jetbrains.edu.learning.ui

import com.intellij.ui.InlineBanner
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

class InlineBannersStack : JPanel() {
  private val verticalGap: Component
    get() = Box.createRigidArea(JBUI.size(0, 4))

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
  }

  fun addInlineBanner(inlineBanner: InlineBanner) {
    add(inlineBanner)
    add(verticalGap)
    parent.revalidate()
    parent.repaint()
  }
}