package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.stepik.SubmissionsManager
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector

object HyperskillSubmissionsManager : SubmissionsManager() {

  fun getAllSubmissions(stepId: Int): MutableList<Submission> {
    return submissions.getOrPut(stepId) { HyperskillConnector.getInstance().getAllSubmissions(stepId) }
  }
}


