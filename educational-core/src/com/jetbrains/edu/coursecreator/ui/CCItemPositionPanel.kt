package com.jetbrains.edu.coursecreator.ui

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.*

/**
 * Allows user to choose where new item should be located
 * if creating action was called with sibling item context.
 *
 * @see [com.jetbrains.edu.coursecreator.actions.CCCreateStudyItemActionBase.getItem]
 */
class CCItemPositionPanel(thresholdName: String) : AdditionalPanel {

  private val beforeButton: JBRadioButton = JBRadioButton("before '$thresholdName'")
  private val afterButton: JBRadioButton = JBRadioButton("after '$thresholdName'", true)

  override fun attach(builder: LayoutBuilder) {
    with(builder) {
      buttonGroup {
        row("Position:") { beforeButton() }
        row("") { afterButton() }
      }
    }
  }

  val indexDelta: Int get() = if (beforeButton.isSelected) 0 else 1
}
