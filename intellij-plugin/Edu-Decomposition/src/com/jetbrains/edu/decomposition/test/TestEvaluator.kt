package com.jetbrains.edu.decomposition.test

import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus

object TestEvaluator {
  // TODO("""Implement evaluate function, suggested idea,
  //  verify if for every node of the generated graph its set of reachable
  //  nodes is contained in the student set of reachable nodes""")
  fun evaluate(expected: Map<String, List<String>>, current: Map<String, List<String>>): CheckResult = CheckResult(CheckStatus.Solved, EduDecompositionBundle.message("action.test.evaluation.success"))
}