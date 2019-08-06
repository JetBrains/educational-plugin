package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.ContestInfo
import com.jetbrains.edu.learning.codeforces.api.ContestPhase

object CodeforcesContestLoader {
  @JvmStatic
  fun getContestInfos(withTrainings: Boolean = false): List<ContestInfo> {
    val result = mutableListOf<ContestInfo>()
    val indicator = ProgressManager.getInstance().progressIndicator
    if (indicator != null && indicator.isCanceled) return emptyList()

    val contestList = CodeforcesConnector.getInstance().getContests(withTrainings) ?: return emptyList()
    if (contestList.isOK) {
      val acceptablePhases = listOf(ContestPhase.CODING, ContestPhase.FINISHED)
      result.addAll(
        contestList.contests.filter {
          acceptablePhases.contains(it.phase)
        }
      )
    }

    return result
  }
}