package com.jetbrains.edu.learning.submissions.ui.segmentedButton

import com.jetbrains.edu.learning.messages.EduCoreBundle

class MySegmentedButtonItem : SegmentedButtonItem("submissions.button.my") {
  override val text: String
    get() = TRIPLE_SPACE + EduCoreBundle.message(nameId) + TRIPLE_SPACE

  companion object {
    /**
     * Workaround: [com.intellij.ui.dsl.builder.SegmentedButton.ItemPresentation] doesn't allow setting custom size of the button,
     * additional space characters allow showing the closest version to the design.
     */
    private val TRIPLE_SPACE = " ".repeat(3)
  }
}