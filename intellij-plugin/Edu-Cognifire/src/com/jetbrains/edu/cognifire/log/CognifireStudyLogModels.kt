package com.jetbrains.edu.cognifire.log

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

data class CognifireStudyLogEntry(
  val courseId: Int?,
  val lessonId: Int?,
  val taskId: Int?,
  val actionType: String,
  val actionId: String,
  val fileName: String,
  val data: ActionData
) {
  override fun toString(): String = jacksonObjectMapper().writeValueAsString(this)
}

data class ActionData(
  val userPrompt: PromptData,
  val userCode: CodeData,
  val generatedPrompt: PromptData,
  val generatedCode: CodeData,
  val promptToCode: PromptToCodeContent,
  val oldPromptToCode: PromptToCodeContent,
  val isGeneratedCodeChanged: Boolean,
)

data class PromptData(
  val prompt: String,
  val code: String,
  val functionSignature: String,
)

data class CodeData(
  val code: String,
)
