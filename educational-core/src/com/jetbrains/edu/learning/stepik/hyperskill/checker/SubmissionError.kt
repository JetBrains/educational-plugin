package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.submissions.Submission

sealed class SubmissionError(val error: String) {
  class NoSubmission(error: String) : SubmissionError(error)
  class WithSubmission(val submission: Submission, error: String) : SubmissionError(error)
}