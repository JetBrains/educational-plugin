package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.courseFormat.CheckResult

interface StderrAnalyzer {
  fun tryToGetCheckResult(stderr: String): CheckResult?
}