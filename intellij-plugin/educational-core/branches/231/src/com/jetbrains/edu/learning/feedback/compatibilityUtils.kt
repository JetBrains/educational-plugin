package com.jetbrains.edu.learning.feedback

import com.intellij.feedback.common.dialog.CommonFeedbackSystemInfoData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

interface SystemDataJsonSerializable {
  fun serializeToJson(json: Json): JsonElement
}

typealias CommonFeedbackSystemData = CommonFeedbackSystemInfoData
