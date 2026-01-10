package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequest

abstract class MarketplaceOpenCourseRequestBase(val courseId: Int, val studyItemId: Int) : OpenInIdeRequest {
  override fun toString(): String = "courseId=$courseId studyItemId=$studyItemId"
  open fun afterProjectOpened(project: Project) {}
}

/**
 * If [studyItemId] is not present in the request, its value should be set to `-1`
 */
class MarketplaceOpenCourseRequest(courseId: Int, studyItemId: Int) : MarketplaceOpenCourseRequestBase(courseId, studyItemId)