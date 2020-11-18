package com.jetbrains.edu.learning

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_URL
import java.net.URL

open class EduBrowser {
  fun browse(url: URL) = browse(url.toExternalForm())

  open fun browse(link: String) {
    BrowserUtil.browse(link)
    countUsage(link)
  }

  fun countUsage(link: String) {
    when {
      link.startsWith(StepikNames.getStepikUrl()) -> {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.STEPIK)
      }
      link.startsWith(CodeforcesNames.CODEFORCES_URL) -> {
        if (link.contains(CodeforcesNames.CODEFORCES_SUBMIT)) {
          EduCounterUsageCollector.codeforcesSubmitSolution()
        }
        else {
          EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.CODEFORCES)
        }
      }
      link.startsWith(HYPERSKILL_DEFAULT_URL) -> {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.JBA)
      }
      else -> {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.EXTERNAL)
      }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(): EduBrowser = service()
  }
}