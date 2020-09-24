package com.jetbrains.edu.learning

import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil

inline fun withTestDialog(dialog: TestDialog, action: () -> Unit) {
  val oldDialog = TestDialogManager.setTestDialog(dialog)
  try {
    action()
  }
  finally {
    UIUtil.dispatchAllInvocationEvents()
    TestDialogManager.setTestDialog(oldDialog)
  }
}

inline fun withTestDialog(dialog: TestInputDialog, action: () -> Unit) {
  val oldDialog = TestDialogManager.setTestInputDialog(dialog)
  try {
    action()
  }
  finally {
    TestDialogManager.setTestInputDialog(oldDialog)
  }
}

typealias TestContext = ThrowableRunnable<Throwable>
