package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import com.jetbrains.edu.learning.newproject.CourseParamsProcessor

data class LtiCourseParams(
  val launchId: String,
  val lmsDescription: String,
  val studyItemId: Int?,
  val ltiCourseraCourse: String?
)

class LtiCourseParamsProcessor : CourseParamsProcessor<LtiCourseParams> {
  override fun findApplicableContext(params: Map<String, String>): LtiCourseParams? {
    val studyItem = params[LTI_STUDY_ITEM_ID]?.toIntOrNull()
    val ltiLaunchId = params[LTI_LAUNCH_ID] ?: return null
    val lmsDescription = params[LTI_LMS_DESCRIPTION] ?: return null
    val ltiCourseraCourse = params[LTI_COURSERA_COURSE]
    return LtiCourseParams(ltiLaunchId, lmsDescription, studyItem, ltiCourseraCourse)
  }

  override fun processCourseParams(project: Project, course: Course, context: LtiCourseParams) {
    val ltiSettings = LTISettingsDTO(
      context.launchId,
      context.lmsDescription,
      LTIOnlineService.STANDALONE,
      context.ltiCourseraCourse?.courseraCourseNameToLink()
    )
    LTISettingsManager.getInstance(project).settings = ltiSettings

    val studyItemId = context.studyItemId ?: -1
    StudyItemSelectionService.getInstance(project).setCurrentStudyItem(studyItemId)
  }

  companion object {
    const val LTI_STUDY_ITEM_ID = "study_item_id"
    const val LTI_LAUNCH_ID = "lti_launch_id"
    const val LTI_LMS_DESCRIPTION = "lti_lms_description"
    const val LTI_COURSERA_COURSE = "lti_coursera_course"
  }
}