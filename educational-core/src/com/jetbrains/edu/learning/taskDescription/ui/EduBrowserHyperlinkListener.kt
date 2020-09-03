package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ui.BrowserHyperlinkListener
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_SUBMIT
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_URL
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL
import javax.swing.event.HyperlinkEvent

open class EduBrowserHyperlinkListener : BrowserHyperlinkListener() {
  override fun hyperlinkActivated(e: HyperlinkEvent) {
    super.hyperlinkActivated(e)

    val host = e.url?.toString() ?: return
    when {
      host.startsWith(STEPIK_URL) -> {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.STEPIK)
      }
      host.startsWith(CODEFORCES_URL) -> {
        if (host.contains(CODEFORCES_SUBMIT)) {
          EduCounterUsageCollector.codeforcesSubmitSolution()
        } else {
          EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.CODEFORCES)
        }
      }
      else -> {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.EXTERNAL)
      }
    }
  }

  companion object {
    @JvmField val INSTANCE: EduBrowserHyperlinkListener = EduBrowserHyperlinkListener()
  }
}