package com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap

import javax.swing.Icon
import com.intellij.ui.LabeledIcon

// BACKCOMPAT: 2023.2
fun getLabeledIcon(icon: Icon, text: String?, iconTextGap: Int): Icon {
  return LabeledIcon(icon, text, "").apply {
    this.iconTextGap = iconTextGap
  }
}