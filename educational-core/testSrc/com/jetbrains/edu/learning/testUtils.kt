@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning

import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.util.ui.UIUtil
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

inline fun <reified T> nullValue(): Matcher<T> = CoreMatchers.nullValue(T::class.java)

fun withFeature(featureId: String, enabled: Boolean, action: () -> Unit) {
  val currentValue = isFeatureEnabled(featureId)
  setFeatureEnabled(featureId, enabled)
  try {
    action()
  }
  finally {
    setFeatureEnabled(featureId, currentValue)
  }
}

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