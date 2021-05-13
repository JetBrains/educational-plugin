package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener


open class SwingToolWindowLinkHandler(project: Project) : ToolWindowLinkHandler(project) {
  val hyperlinkListener: HyperlinkListener
    get() = HyperlinkListener { event ->
      if (event.eventType != HyperlinkEvent.EventType.ACTIVATED) return@HyperlinkListener
      return@HyperlinkListener processEvent(event)
    }

  open fun processEvent(event: HyperlinkEvent) {
    process(event.description)
  }
}