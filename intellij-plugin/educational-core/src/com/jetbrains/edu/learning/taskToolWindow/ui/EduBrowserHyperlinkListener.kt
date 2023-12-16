package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ui.BrowserHyperlinkListener
import com.jetbrains.edu.learning.EduBrowser
import javax.swing.event.HyperlinkEvent

open class EduBrowserHyperlinkListener : BrowserHyperlinkListener() {
  override fun hyperlinkActivated(e: HyperlinkEvent) {
    super.hyperlinkActivated(e)

    val host = e.url?.toString() ?: return
    EduBrowser.getInstance().countUsage(host)
  }

  companion object {
    val INSTANCE: EduBrowserHyperlinkListener = EduBrowserHyperlinkListener()
  }
}