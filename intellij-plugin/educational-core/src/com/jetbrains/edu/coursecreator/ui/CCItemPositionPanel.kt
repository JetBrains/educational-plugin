package com.jetbrains.edu.coursecreator.ui

import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind
import com.jetbrains.edu.learning.messages.EduCoreBundle

/**
 * Allows user to choose where new item should be located
 * if creating action was called with sibling item context.
 *
 * @see [com.jetbrains.edu.coursecreator.actions.CCCreateStudyItemActionBase.showCreationUI]
 */
class CCItemPositionPanel(private val thresholdName: String) : AdditionalPanel {

  override fun attach(panel: Panel) {
    with(panel) {
      buttonsGroup {
        row("${EduCoreBundle.message("label.item.position")}:") {
          radioButton(EduCoreBundle.message("radio.item.position.before", thresholdName), BEFORE_DELTA)
        }
        row("") {
          radioButton(EduCoreBundle.message("radio.item.position.after", thresholdName), AFTER_DELTA)
        }
      }.bind(::indexDelta)
    }
  }

  var indexDelta: Int = AFTER_DELTA
    private set

  // TODO: move these constants in better place
  companion object {
    const val BEFORE_DELTA = 0
    const val AFTER_DELTA = 1
  }
}
