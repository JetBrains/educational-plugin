package com.jetbrains.edu.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequestBase

class LTIOpenCourseRequest(courseId: Int, studyItemId: Int, val ltiSettingsDTO: LTISettingsDTO) : MarketplaceOpenCourseRequestBase(courseId, studyItemId) {
  override fun toString(): String = "courseId=$courseId studyItemId=$studyItemId ltiLaunchId=${ltiSettingsDTO.launchId}"

  override fun afterProjectOpened(project: Project) {
    LTISettingsManager.getInstance(project).settings = ltiSettingsDTO
  }
}