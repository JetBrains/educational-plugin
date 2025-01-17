package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequest
import com.jetbrains.edu.learning.marketplace.lti.LTISettingsDTO

/**
 * If `taskId` is not present in the request, its value should be set to `-1`
 */
class MarketplaceOpenCourseRequest(val courseId: Int, val taskId: Int, val ltiSettingsDTO: LTISettingsDTO? = null) : OpenInIdeRequest {
  override fun toString(): String = "courseId=$courseId taskId=$taskId ltiLaunchId=${ltiSettingsDTO?.launchId}"
}