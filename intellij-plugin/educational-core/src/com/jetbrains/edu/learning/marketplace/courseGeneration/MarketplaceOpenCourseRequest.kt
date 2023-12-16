package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequest

class MarketplaceOpenCourseRequest(val courseId: Int) : OpenInIdeRequest {
  override fun toString(): String = "courseId=$courseId"
}