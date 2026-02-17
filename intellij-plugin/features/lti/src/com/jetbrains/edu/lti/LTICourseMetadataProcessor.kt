package com.jetbrains.edu.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState

data class LTICourseParams(
  val launchId: String,
  val lmsDescription: String?,
  val studyItemId: Int?,
  val ltiCourseraCourse: String?
)

class LTICourseMetadataProcessor : CourseMetadataProcessor<LTICourseParams> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): LTICourseParams? {
    val studyItem = rawMetadata[LTI_STUDY_ITEM_ID]?.toIntOrNull()
    val ltiLaunchId = rawMetadata[LTI_LAUNCH_ID] ?: return null
    val lmsDescription = rawMetadata[LTI_LMS_DESCRIPTION]
    val ltiCourseraCourse = rawMetadata[LTI_COURSERA_COURSE]
    return LTICourseParams(ltiLaunchId, lmsDescription, studyItem, ltiCourseraCourse)
  }

  override fun processMetadata(project: Project, course: Course, metadata: LTICourseParams, courseProjectState: CourseProjectState) {
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