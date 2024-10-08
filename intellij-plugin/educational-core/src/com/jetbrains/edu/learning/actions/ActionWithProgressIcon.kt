package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.util.function.Supplier
import javax.swing.JPanel

abstract class ActionWithProgressIcon : AnAction {
  var spinnerPanel: JPanel? = null
    private set

  protected constructor() : super()

  protected constructor(actionText: Supplier<@ActionText String>) : super(actionText)

  @RequiresEdt
  protected fun setUpSpinnerPanel(@NonNls message: String) {
    val asyncProcessIcon = AsyncProcessIcon(message)
    val iconPanel = JPanel(BorderLayout()).apply {
      add(asyncProcessIcon, BorderLayout.WEST)
      border = JBUI.Borders.empty(8, 6, 0, 10)
      isVisible = false
    }
    spinnerPanel = iconPanel
  }

  @RequiresEdt
  protected fun processStarted() {
    spinnerPanel?.isVisible = true
  }

  @RequiresEdt
  protected fun processFinished() {
    spinnerPanel?.isVisible = false
  }
}
