package com.jetbrains.edu.learning

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog

open class EduTestDialog : TestDialog {

  var shownMessage: String? = null

  override fun show(message: String): Int {
    shownMessage = message
    return Messages.OK
  }

  /**
   * Checks if dialog was shown.
   * If [expectedMessage] is not null, also checks that shown message equals expected one
   */
  @JvmOverloads
  fun checkWasShown(expectedMessage: String? = null) {
    check(shownMessage != null) {
      "Message dialog should be shown"
    }
    if (expectedMessage != null) {
      check(shownMessage == expectedMessage) {
        "Expected `$expectedMessage` but `$shownMessage` was shown"
      }
    }
  }
}

inline fun <T: TestDialog> withTestDialog(dialog: T, action: () -> Unit): T {
  val oldDialog = Messages.setTestDialog(dialog)
  try {
    action()
  } finally {
    Messages.setTestDialog(oldDialog)
  }
  return dialog
}
