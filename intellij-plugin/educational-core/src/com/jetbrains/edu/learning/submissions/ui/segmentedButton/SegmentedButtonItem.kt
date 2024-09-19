package com.jetbrains.edu.learning.submissions.ui.segmentedButton

import com.jetbrains.edu.learning.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

sealed class SegmentedButtonItem(@PropertyKey(resourceBundle = BUNDLE) protected val nameId: String) {

  var isEnabled: Boolean = true

  var toolTipText: String? = null

  abstract val text: String
}