package com.jetbrains.edu.coursecreator.ui

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.*

class CCItemPositionPanel(thresholdName: String) {

  private val beforeButton: JBRadioButton = JBRadioButton("before '$thresholdName'")
  private val afterButton: JBRadioButton = JBRadioButton("after '$thresholdName'", true)

  fun attach(builder: LayoutBuilder) {
    with(builder) {
      buttonGroup {
        row("Position:") { beforeButton() }
        row("") { afterButton() }
      }
    }
  }

  val indexDelta: Int get() = if (beforeButton.isSelected) 0 else 1
}
