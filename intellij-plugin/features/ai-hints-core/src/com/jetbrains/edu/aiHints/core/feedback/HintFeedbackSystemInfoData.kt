package com.jetbrains.edu.aiHints.core.feedback

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class CodeHintFeedbackSystemInfoData(
  val commonSystemInfo: CommonFeedbackSystemData,
  val codeHintFeedbackInfo: CodeHintFeedbackInfoData
) : SystemDataJsonSerializable {
  override fun serializeToJson(json: Json): JsonElement = json.encodeToJsonElement(this)
}

@Serializable
data class TextHintFeedbackSystemInfoData(
  val commonSystemInfo: CommonFeedbackSystemData,
  val textHintFeedbackInfo: TextHintFeedbackInfoData
) : SystemDataJsonSerializable {
  override fun serializeToJson(json: Json): JsonElement = json.encodeToJsonElement(this)
}