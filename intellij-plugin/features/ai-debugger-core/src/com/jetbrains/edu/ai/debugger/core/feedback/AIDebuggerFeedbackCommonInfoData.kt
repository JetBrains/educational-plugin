package com.jetbrains.edu.ai.debugger.core.feedback

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
import com.jetbrains.edu.ai.debugger.core.service.TestInfo
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class AIDebuggerFeedbackCommonInfoData(
  val commonSystemInfo: CommonFeedbackSystemData,

  val courseId: Int,
  val courseUpdateVersion: Int,
  val courseName: String,
  val taskId: Int,
  val taskName: String,
  val studentSolution: Map<String, String>,

  val testName: String,
  val testErrorMessage: String,
  val testExpectedOutput: String,
  val testText: String,

  val finalBreakpoints: Map<String, List<Int>>,
  val intermediateBreakpoints: Map<String, List<Int>>,
  val breakpointHints: Map<String, List<Pair<Int, String>>>,
) : SystemDataJsonSerializable {
  override fun serializeToJson(json: Json): JsonElement = json.encodeToJsonElement(this)

  companion object {
    @JvmStatic
    fun create(
      commonSystemInfo: CommonFeedbackSystemData,
      task: Task,
      userSolution: Map<String, String>,
      testInfo: TestInfo,
      finalBreakpoints: List<Breakpoint>,
      intermediateBreakpoints: Map<String, List<Int>>,
      breakpointHints: List<BreakpointHintDetails>
    ): AIDebuggerFeedbackCommonInfoData = AIDebuggerFeedbackCommonInfoData(
      commonSystemInfo = commonSystemInfo,
      courseId = task.course.id,
      courseUpdateVersion = task.course.marketplaceCourseVersion,
      courseName = task.course.name,
      taskId = task.id,
      taskName = task.name,
      studentSolution = userSolution,
      testName = testInfo.name,
      testErrorMessage = testInfo.errorMessage,
      testExpectedOutput = testInfo.expectedOutput,
      testText = testInfo.text,
      finalBreakpoints = finalBreakpoints.groupBy { it.fileName }.mapValues { (_, breakpoints) -> breakpoints.map { it.lineNumber } },
      intermediateBreakpoints = intermediateBreakpoints,
      breakpointHints = breakpointHints.groupBy { it.fileName }.mapValues { (_, hints) -> hints.map { it.lineNumber to it.hint } }
    )
  }
}
