package com.jetbrains.edu.ai.error.explanation

import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.ui.RoundedWrapper

class ErrorEditorPanel(text: String, onClose: () -> Unit) : Wrapper() {
  init {
    val panel = panel {
      row {
        text(text)
      }
      row {
        link("Close") { onClose() }
      }
    }
    setContent(RoundedWrapper(panel, 8))
  }
}