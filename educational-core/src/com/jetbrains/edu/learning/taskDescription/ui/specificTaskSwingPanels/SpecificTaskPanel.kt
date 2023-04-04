package com.jetbrains.edu.learning.taskDescription.ui.specificTaskSwingPanels

import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI

abstract class SpecificTaskPanel: Wrapper() {
  protected val specificPanelInsets = JBUI.insets(15, 0, 10, 10)
}

