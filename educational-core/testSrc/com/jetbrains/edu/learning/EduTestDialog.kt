package com.jetbrains.edu.learning

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestInputDialog

sealed class EduTestDialogBase<T> {

  var shownMessage: String? = null

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

  protected abstract val defaultReturnValue: T
}

open class EduTestDialog(override val defaultReturnValue: Int = Messages.OK) : EduTestDialogBase<Int>(), TestDialog {

  override fun show(message: String): Int {
    shownMessage = message
    return defaultReturnValue
  }
}

open class EduTestInputDialog(override val defaultReturnValue: String) : EduTestDialogBase<String>(), TestInputDialog {

  override fun show(message: String): String {
    shownMessage = message
    return defaultReturnValue
  }
}

inline fun <T: EduTestDialogBase<*>> withTestDialog(dialog: T, action: () -> Unit): T {

  when (dialog) {
    is EduTestDialog -> {
      val oldDialog = Messages.setTestDialog(dialog)
      try {
        action()
      } finally {
        Messages.setTestDialog(oldDialog)
      }
    }
    is EduTestInputDialog -> {
      val oldDialog = Messages.setTestInputDialog(dialog)
      try {
        action()
      } finally {
        Messages.setTestInputDialog(oldDialog)
      }
    }
    else -> error("Unexpected dialog type: `${dialog.javaClass.canonicalName}`")
  }

  return dialog
}
