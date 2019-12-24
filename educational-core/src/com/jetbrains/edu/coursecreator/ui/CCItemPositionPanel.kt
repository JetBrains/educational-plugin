package com.jetbrains.edu.coursecreator.ui

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.*

/**
 * Allows user to choose where new item should be located
 * if creating action was called with sibling item context.
 *
 * @see [com.jetbrains.edu.coursecreator.actions.CCCreateStudyItemActionBase.showCreationUI]
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

  val indexDelta: Int get() = if (beforeButton.isSelected) BEFORE_DELTA else AFTER_DELTA

  // TODO: move these constants in better place
  companion object {
    const val BEFORE_DELTA = 0
    const val AFTER_DELTA = 1
  }
}
