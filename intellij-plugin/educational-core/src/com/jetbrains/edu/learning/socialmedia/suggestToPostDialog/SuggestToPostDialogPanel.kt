package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

import com.intellij.openapi.ui.ValidationInfo
import java.awt.LayoutManager
import javax.swing.JPanel

abstract class SuggestToPostDialogPanel : JPanel {
  constructor(layout: LayoutManager) : super(layout)
  constructor() : super()

  /**
   * Provides post text
   */
  abstract val message: String

  open fun doValidate(): ValidationInfo? {
    return null
  }
}
