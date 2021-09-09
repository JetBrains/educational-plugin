package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.util.function.Supplier
import javax.swing.JPanel

abstract class ActionWithProgressIcon : AnAction {
  var processPanel: JPanel? = null
    private set

  @Suppress("unused") // This constructor is used when action is called via Actions list
  protected constructor(actionText: Supplier<String>) : super(actionText)
  protected constructor(actionText: Supplier<String>, descriptionText: Supplier<String>) : super(actionText, descriptionText, null)

  protected fun setUpProcessPanel(@NonNls message: String) {
    val asyncProcessIcon = AsyncProcessIcon(message)
    val iconPanel = JPanel(BorderLayout()).apply {
      add(asyncProcessIcon, BorderLayout.WEST)
      border = JBUI.Borders.empty(8, 6, 0, 10)
      isVisible = false
    }
    processPanel = iconPanel
  }

  protected fun processStarted() {
    processPanel?.isVisible = true
  }

  protected fun processFinished() {
    processPanel?.isVisible = false
  }
}