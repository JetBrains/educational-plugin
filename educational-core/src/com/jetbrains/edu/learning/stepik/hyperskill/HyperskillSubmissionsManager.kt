package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.stepik.StepikSubmissionsManager
import com.jetbrains.edu.learning.stepik.SubmissionsManager
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

object HyperskillSubmissionsManager : SubmissionsManager() {
  fun fillSubmissions() {
    if (HyperskillSettings.INSTANCE.account == null) return
    if (!submissions.isEmpty()) return
    val stepikSubmissions = StepikSubmissionsManager.getAllSubmissions(186010)
    val submission: Submission? = if (stepikSubmissions.isNotEmpty()) {
      stepikSubmissions[0]
    }
    else {
      null
    }

    if (submission != null) {
      putToSubmissions(5659, mutableListOf(submission))
    }
    else {
      putToSubmissions(5659, mutableListOf())
    }
  }

  fun getAllSubmissions(stageId: Int): MutableList<Submission> {
    return submissions.getOrPut(stageId) { HyperskillConnector.getInstance().getAllSubmissions(stageId) }
  }
}


