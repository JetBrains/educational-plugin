package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask
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

  @VisibleForTesting
  fun createStringSubmission(task: StringTask, attemptId: Int, project: Project): Submission {
    val submission = Submission()
    submission.attempt = attemptId
    submission.step = task.id
    val reply = Reply()
    reply.text = task.getInputAnswer(project)
    submission.reply = reply
    return submission
  }
}