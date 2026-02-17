package com.jetbrains.edu.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequestBase

class LTIOpenCourseRequest(courseId: Int, studyItemId: Int, val ltiSettingsDTO: LTISettingsDTO? = null) : MarketplaceOpenCourseRequestBase(courseId, studyItemId) {
  override fun toString(): String = "courseId=$courseId studyItemId=$studyItemId ltiLaunchId=${ltiSettingsDTO?.launchId}"

  override fun afterProjectOpened(project: Project) {
    val ltiSettings = ltiSettingsDTO
    if (ltiSettings != null) {
      LTISettingsManager.getInstance(project).settings = ltiSettings
    }
  }
}