package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState

data class LtiCourseParams(
  val launchId: String,
  val lmsDescription: String,
  val studyItemId: Int?,
  val ltiCourseraCourse: String?
)

class LtiCourseMetadataProcessor : CourseMetadataProcessor<LtiCourseParams> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): LtiCourseParams? {
    val studyItem = rawMetadata[LTI_STUDY_ITEM_ID]?.toIntOrNull()
    val ltiLaunchId = rawMetadata[LTI_LAUNCH_ID] ?: return null
    val lmsDescription = rawMetadata[LTI_LMS_DESCRIPTION] ?: return null
    val ltiCourseraCourse = rawMetadata[LTI_COURSERA_COURSE]
    return LtiCourseParams(ltiLaunchId, lmsDescription, studyItem, ltiCourseraCourse)
  }

  override fun processMetadata(project: Project, course: Course, metadata: LtiCourseParams, courseProjectState: CourseProjectState) {
    val ltiSettings = LTISettingsDTO(
      metadata.launchId,
      metadata.lmsDescription,
      LTIOnlineService.STANDALONE,
      metadata.ltiCourseraCourse?.courseraCourseNameToLink()
    )
    LTISettingsManager.getInstance(project).settings = ltiSettings

    val studyItemId = metadata.studyItemId ?: -1
    StudyItemSelectionService.getInstance(project).setCurrentStudyItem(studyItemId)
  }

  companion object {
    const val LTI_STUDY_ITEM_ID = "study_item_id"
    const val LTI_LAUNCH_ID = "lti_launch_id"
    const val LTI_LMS_DESCRIPTION = "lti_lms_description"
    const val LTI_COURSERA_COURSE = "lti_coursera_course"
  }
}