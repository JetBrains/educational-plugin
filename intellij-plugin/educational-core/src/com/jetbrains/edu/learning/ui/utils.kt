package com.jetbrains.edu.learning.ui

import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI
import com.intellij.ui.ClientProperty
import javax.swing.JButton

/**
 * Makes the button blue like a default button in dialogs
 */
var JButton.isDefault: Boolean
  get() = ClientProperty.isTrue(this, DarculaButtonUI.DEFAULT_STYLE_KEY)
  set(value) {
    if (value) {
      ClientProperty.put(this, DarculaButtonUI.DEFAULT_STYLE_KEY, true)
    }
    else {
      ClientProperty.remove(this, DarculaButtonUI.DEFAULT_STYLE_KEY)
    }
  }
