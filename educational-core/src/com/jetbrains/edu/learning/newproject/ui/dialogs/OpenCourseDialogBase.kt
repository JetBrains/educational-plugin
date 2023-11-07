package com.jetbrains.edu.learning.newproject.ui.dialogs

import com.intellij.CommonBundle
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.UIUtil
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

abstract class OpenCourseDialogBase : DialogWrapper(true) {

  override fun createActions(): Array<out Action> {
    val closeAction = object : AbstractAction(UIUtil.replaceMnemonicAmpersand(CommonBundle.message("button.close"))) {
      override fun actionPerformed(e: ActionEvent) {
        close()
      }
    }

    return arrayOf(closeAction)
  }

  fun close() {
    close(CANCEL_EXIT_CODE)
  }

  override fun getStyle(): DialogStyle {
    return DialogStyle.COMPACT
  }
}
