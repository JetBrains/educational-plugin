package com.jetbrains.edu.learning.feedback

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable

@Suppress("UnstableApiUsage")
interface JbAcademyFeedbackSystemInfoData : SystemDataJsonSerializable {
  val commonSystemInfo: CommonFeedbackSystemData
  val courseFeedbackInfoData: CourseFeedbackInfoData
}
