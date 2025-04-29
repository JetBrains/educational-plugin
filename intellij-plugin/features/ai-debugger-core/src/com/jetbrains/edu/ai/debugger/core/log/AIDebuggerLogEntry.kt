package com.jetbrains.edu.ai.debugger.core.log

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.educational.ml.debugger.dto.BreakpointHintDetails
import com.jetbrains.educational.ml.debugger.dto.CodeFixDetails

data class AIDebuggerLogEntry(
  val task: TaskData,
  val actionType: String,
  val testResult: CheckResult? = null,
  val testText: String = "",
  val userCode: String = "",
  val error: String = "",
  val fixes: List<CodeFixDetails> = emptyList(),
  val intermediateBreakpoints: Map<String, List<Int>> = emptyMap(),
  val breakpointHints: List<BreakpointHintDetails> = emptyList(),
) {
  override fun toString(): String = jacksonObjectMapper().writeValueAsString(this)
}

data class TaskData(
  val courseId: Int,
  val lessonId: Int,
  val taskId: Int,
)
