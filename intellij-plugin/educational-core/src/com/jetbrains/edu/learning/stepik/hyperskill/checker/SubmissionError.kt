package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission

sealed class SubmissionError(val error: String) {
  class NoSubmission(error: String) : SubmissionError(error)
  class WithSubmission(val submission: StepikBasedSubmission, error: String) : SubmissionError(error)
}