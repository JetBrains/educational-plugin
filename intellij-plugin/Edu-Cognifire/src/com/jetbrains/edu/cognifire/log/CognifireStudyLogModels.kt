package com.jetbrains.edu.cognifire.log

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

data class CognifireStudyLogEntry(
  val courseId: Int,
  val lessonId: Int,
  val taskId: Int,
  val actionType: String,
  val actionId: String,
  val fileName: String,
  val data: ActionData
) {
  override fun toString(): String = jacksonObjectMapper().writeValueAsString(this)
}

data class ActionData(
  val givenPrompt: PromptData,
  val generatedCode: String,
  val promptToCode: PromptToCodeContent
)

data class PromptData(
  val prompt: String,
  val code: String,
  val functionSignature: String,
)
