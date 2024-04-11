@file:Suppress("UnstableApiUsage")

package com.jetbrains.edu.learning.feedback

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class JbAcademyStudentFeedbackSystemInfoData(
  override val commonSystemInfo: CommonFeedbackSystemData,
  override val courseFeedbackInfoData: CourseFeedbackInfoData,
  val taskPath: String,
) : JbAcademyFeedbackSystemInfoData {

  override fun serializeToJson(json: Json): JsonElement {
    return json.encodeToJsonElement(this)
  }

  override fun toString(): String {
    return buildString {
      appendLine(courseFeedbackInfoData.toString())
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id"))
      appendLine(courseFeedbackInfoData.courseId.toString())
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.task"))
      appendLine(taskPath)
      appendLine()
      appendLine(commonSystemInfo.toString())
    }
  }
}
