package com.jetbrains.edu.learning.feedback

@Suppress("UnstableApiUsage")
interface JbAcademyFeedbackSystemInfoData : SystemDataJsonSerializable {
  val commonSystemInfo: CommonFeedbackSystemData
  val courseFeedbackInfoData: CourseFeedbackInfoData
}
