@file:Suppress("UnstableApiUsage")

package com.jetbrains.edu.coursecreator.feedback

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.jetbrains.edu.learning.feedback.CourseFeedbackInfoData
import com.jetbrains.edu.learning.feedback.JbAcademyFeedbackSystemInfoData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class JbAcademyCCFeedbackSystemInfoData(
  override val commonSystemInfo: CommonFeedbackSystemData,
  override val courseFeedbackInfoData: CourseFeedbackInfoData
) : JbAcademyFeedbackSystemInfoData {

  override fun serializeToJson(json: Json): JsonElement {
    return json.encodeToJsonElement(this)
  }

  override fun toString(): String {
    return buildString {
      appendLine(courseFeedbackInfoData.toString())
      appendLine()
      appendLine(commonSystemInfo.toString())
    }
  }
}
