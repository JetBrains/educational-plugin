package com.jetbrains.edu.learning.socialmedia.twitter.ui

import com.intellij.openapi.ui.ValidationInfo
import java.awt.LayoutManager
import javax.swing.JPanel

/**
 * Class provides structure for twitter dialog panel
 */
abstract class TwitterDialogPanel : JPanel {
  constructor(layout: LayoutManager) : super(layout)
  constructor() : super()

  /**
   * Provides tweet text
   */
  abstract val message: String

  open fun doValidate(): ValidationInfo? {
    return null
  }
}
