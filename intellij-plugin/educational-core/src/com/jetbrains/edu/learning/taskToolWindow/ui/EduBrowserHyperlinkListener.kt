package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ui.BrowserHyperlinkListener
import com.jetbrains.edu.learning.EduBrowser
import javax.swing.event.HyperlinkEvent

open class EduBrowserHyperlinkListener : BrowserHyperlinkListener() {
  override fun hyperlinkActivated(e: HyperlinkEvent) {
    super.hyperlinkActivated(e)
    EduBrowser.getInstance().countUsage()
  }

  companion object {
    val INSTANCE: EduBrowserHyperlinkListener = EduBrowserHyperlinkListener()
  }
}