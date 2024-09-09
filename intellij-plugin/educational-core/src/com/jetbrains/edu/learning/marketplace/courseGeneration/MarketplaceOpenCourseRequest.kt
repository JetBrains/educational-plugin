package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequest

/**
 * If `taskId` is not present in the request, its value should be set to `-1`
 */
class MarketplaceOpenCourseRequest(val courseId: Int, val taskId: Int) : OpenInIdeRequest {
  override fun toString(): String = "courseId=$courseId taskId=$taskId"
}