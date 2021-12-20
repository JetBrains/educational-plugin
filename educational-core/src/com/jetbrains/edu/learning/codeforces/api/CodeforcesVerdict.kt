package com.jetbrains.edu.learning.codeforces.api

import com.jetbrains.edu.learning.EduNames.CORRECT
import com.jetbrains.edu.learning.EduNames.WRONG
import com.jetbrains.edu.learning.courseFormat.CheckStatus

enum class CodeforcesVerdict(val stringVerdict: String) {
  OK(CORRECT),
  FAILED(WRONG),
  PARTIAL(WRONG),
  COMPILATION_ERROR(WRONG),
  RUNTIME_ERROR(WRONG),
  WRONG_ANSWER(WRONG),
  PRESENTATION_ERROR(WRONG),
  TIME_LIMIT_EXCEEDED(WRONG),
  MEMORY_LIMIT_EXCEEDED(WRONG),
  IDLENESS_LIMIT_EXCEEDED(WRONG),
  SECURITY_VIOLATED(WRONG),
  CRASHED(WRONG),
  INPUT_PREPARATION_CRASHED(WRONG),
  CHALLENGED(WRONG),
  SKIPPED(WRONG),
  TESTING(""),
  REJECTED(WRONG);

  fun toCheckStatus(): CheckStatus = when (this) {
    OK -> CheckStatus.Solved
    TESTING -> CheckStatus.Unchecked
    else -> CheckStatus.Failed
  }

}