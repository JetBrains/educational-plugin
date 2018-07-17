package com.jetbrains.edu.learning.checkio.ui

import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.checkio.CheckiONames

class CheckiOOptionsUIProvider {
  val loginLabel = JBLabel()
  val loginLink = HoverHyperlinkLabel("")

  val panel = panel(LCFlags.disableMagic, title = CheckiONames.CHECKIO_NAME) {
    row(loginLabel) {
      loginLink(CCFlags.growX, CCFlags.pushX)
    }
  }
}
