package com.jetbrains.edu.aiHints.core.feedback.data

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class ErrorHintFeedbackSystemInfoData(
  val commonSystemInfo: CommonFeedbackSystemData,
  val hintFeedbackInfo: ErrorHintFeedbackInfoData
) : SystemDataJsonSerializable {
  override fun serializeToJson(json: Json): JsonElement = json.encodeToJsonElement(this)
}