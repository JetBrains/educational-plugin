package com.jetbrains.edu.ai.debugger.core.feedback

import com.jetbrains.edu.ai.debugger.core.service.TestInfo
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails

data class AIDebugContext(
  val task: Task,
  val userSolution: Map<String, String>,
  val testInfo: TestInfo,
  val finalBreakpoints: List<Breakpoint>,
  val intermediateBreakpoints: Map<String, List<Int>>,
  val breakpointHints: List<BreakpointHintDetails>
)
