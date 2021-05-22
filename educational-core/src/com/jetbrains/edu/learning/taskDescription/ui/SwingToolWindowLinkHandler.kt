package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener


open class SwingToolWindowLinkHandler(project: Project) : ToolWindowLinkHandler(project), HyperlinkListener {
  open fun processEvent(event: HyperlinkEvent) {
    process(event.description)
  }

  override fun hyperlinkUpdate(event: HyperlinkEvent) {
    if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
      processEvent(event)
    }
  }
}