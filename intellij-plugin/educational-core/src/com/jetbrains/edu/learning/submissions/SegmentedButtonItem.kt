package com.jetbrains.edu.learning.submissions

import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

sealed class SegmentedButtonItem(@PropertyKey(resourceBundle = BUNDLE) protected val nameId: String) {

  var isEnabled: Boolean = true

  var toolTipText: String? = null

  abstract val text: String
}

class MySegmentedButton : SegmentedButtonItem("submissions.button.my") {
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

class CommunitySegmentedButton : SegmentedButtonItem("submissions.button.community") {
  override val text: String
    get() = EduCoreBundle.message(nameId)
}
