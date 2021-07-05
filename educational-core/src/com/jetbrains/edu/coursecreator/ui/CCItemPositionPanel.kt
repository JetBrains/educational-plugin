package com.jetbrains.edu.coursecreator.ui

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.messages.EduCoreBundle

/**
 * Allows user to choose where new item should be located
 * if creating action was called with sibling item context.
 *
 * @see [com.jetbrains.edu.coursecreator.actions.CCCreateStudyItemActionBase.showCreationUI]
 */
class CCItemPositionPanel(thresholdName: String) : AdditionalPanel {

  private val beforeButton: JBRadioButton = JBRadioButton(EduCoreBundle.message("radio.item.position.before", thresholdName))
  private val afterButton: JBRadioButton = JBRadioButton(EduCoreBundle.message("radio.item.position.after", thresholdName))

  override fun attach(builder: LayoutBuilder) {
    with(builder) {
      buttonGroup {
        row("${EduCoreBundle.message("label.item.position")}:") { beforeButton() }
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
