@file:Suppress("UnstableApiUsage")

package com.jetbrains.edu.learning

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.util.ui.UIUtil

inline fun withTestDialog(dialog: TestDialog, action: () -> Unit) {
  val oldDialog = Messages.setTestDialog(dialog)
  try {
    action()
  }
  finally {
    UIUtil.dispatchAllInvocationEvents()
    Messages.setTestDialog(oldDialog)
  }
}

inline fun withTestDialog(dialog: TestInputDialog, action: () -> Unit) {
  val oldDialog = Messages.setTestInputDialog(dialog)
  try {
    action()
  }
  finally {
    Messages.setTestInputDialog(oldDialog)
  }
}

typealias TestContext = Unit
