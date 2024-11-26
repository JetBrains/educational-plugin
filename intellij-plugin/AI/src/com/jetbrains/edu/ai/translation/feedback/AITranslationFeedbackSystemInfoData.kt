package com.jetbrains.edu.ai.translation.feedback

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class AITranslationFeedbackSystemInfoData(
  val commonSystemInfo: CommonFeedbackSystemData,
  val aiTranslationFeedbackInfoData: AITranslationFeedbackInfoData,
) : SystemDataJsonSerializable {
  override fun serializeToJson(json: Json): JsonElement = json.encodeToJsonElement(this)
}