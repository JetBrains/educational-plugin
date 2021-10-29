package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.submissions.Submission

object HyperskillSubmissionProvider {
  @VisibleForTesting
  fun createEduSubmission(task: Task, attempt: Attempt, files: List<SolutionFile>, feedback: String): Submission {
    val score = if (task.status == CheckStatus.Solved) "1" else "0"
    return Submission(score, attempt.id, files, null, feedback)
  }

  @VisibleForTesting
  fun createRemoteEduTaskSubmission(checkProfile: String, attempt: Attempt, files: List<SolutionFile>): Submission {
    val reply = Reply()
    reply.checkProfile = checkProfile
    reply.solution = files
    val submission = Submission()
    submission.attempt = attempt.id
    submission.reply = reply
    return submission
  }
}