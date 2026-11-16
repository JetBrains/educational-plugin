package com.jetbrains.edu.learning.actions

import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Progress icon shown next to an action button in the check panel while the action is running.
 *
 * The panel is created lazily on first access, so the owning action can be instantiated on any thread.
 *
 * @see com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanelButtonComponent
 */
class ActionProgressIcon(@NonNls private val message: String) {

  val panel: JPanel by lazy {
    JPanel(BorderLayout()).apply {
      add(AsyncProcessIcon(message), BorderLayout.WEST)
      border = JBUI.Borders.empty(8, 6, 0, 10)
      isVisible = false
    }
  }

  @RequiresEdt
  fun processStarted() {
    panel.isVisible = true
  }

  @RequiresEdt
  fun processFinished() {
    panel.isVisible = false
  }
}
