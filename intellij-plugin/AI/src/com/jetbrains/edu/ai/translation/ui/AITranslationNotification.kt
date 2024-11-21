package com.jetbrains.edu.ai.translation.ui

import com.intellij.openapi.util.NlsContexts.LinkLabel
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.ui.EditorNotificationPanel
import javax.swing.JComponent

class AITranslationNotification(
  status: Status,
  @NotificationContent private val messageText: String,
  private val parentComponent: JComponent
) : EditorNotificationPanel(status) {
  init {
    text = "<html>$messageText</html>"
    setCloseAction(::close)
  }

  fun addActionLabel(actionLabel: ActionLabel) {
    createActionLabel(actionLabel.name) {
      close()
      actionLabel.action()
    }
  }

  fun close() {
    parentComponent.remove(this)
    parentComponent.doLayout()
    parentComponent.revalidate()
    parentComponent.repaint()
  }

  data class ActionLabel(
    @LinkLabel val name: String,
    val action: () -> Unit
  )
}