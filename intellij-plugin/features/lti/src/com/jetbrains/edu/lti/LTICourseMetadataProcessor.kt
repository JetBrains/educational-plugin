package com.jetbrains.edu.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState

data class LTICourseParams(
  val launchId: String,
  val lmsDescription: String?,
  val ltiCourseraCourse: String?
)

class LTICourseMetadataProcessor : CourseMetadataProcessor<LTICourseParams> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): LTICourseParams? {
    val ltiLaunchId = rawMetadata[LTI_LAUNCH_ID] ?: return null
    val lmsDescription = rawMetadata[LTI_LMS_DESCRIPTION]
    val ltiCourseraCourse = rawMetadata[LTI_COURSERA_COURSE]
    return LTICourseParams(ltiLaunchId, lmsDescription, ltiCourseraCourse)
  }

  override fun processMetadata(project: Project, course: Course, metadata: LTICourseParams, courseProjectState: CourseProjectState) {
    val ltiSettings = LTISettingsDTO(
      metadata.launchId,
      metadata.lmsDescription,
      LTIOnlineService.STANDALONE,
      metadata.ltiCourseraCourse?.courseraCourseNameToLink()
    )
    LTISettingsManager.getInstance(project).settings = ltiSettings
  }

  companion object {
    const val LTI_LAUNCH_ID = "lti_launch_id"
    const val LTI_LMS_DESCRIPTION = "lti_lms_description"
    const val LTI_COURSERA_COURSE = "lti_coursera_course"
  }
}