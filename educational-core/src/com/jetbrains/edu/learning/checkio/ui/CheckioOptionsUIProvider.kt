package com.jetbrains.edu.learning.checkio.ui

import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.*

class CheckioOptionsUIProvider {
  val loginLabel = JBLabel()
  val loginLink = HoverHyperlinkLabel("")

  val panel = panel(LCFlags.disableMagic, title = "CheckiO") {
    row(loginLabel) {
      loginLink(CCFlags.growX, CCFlags.pushX)
    }
  }
}
