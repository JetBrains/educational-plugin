package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequest

class MarketplaceOpenLtiLinkCourseRequest(
  val courseId: Int,
  val updateVersion: Int,
  val taskEduId: Int,
  val launchId: String
) : OpenInIdeRequest {
  override fun toString(): String = "courseId=$courseId updateVersion=$updateVersion taskEduId=$taskEduId launchId=$launchId"
}