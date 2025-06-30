package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import com.jetbrains.edu.learning.newproject.CourseParamsProcessor

data class LtiCourseParams(val launchId: String, val lmsDescription: String, val studyItemId: Int?)

class LtiCourseParamsProcessor : CourseParamsProcessor<LtiCourseParams> {
  override fun findApplicableContext(params: Map<String, String>): LtiCourseParams? {
    val studyItem = params[LTI_STUDY_ITEM_ID]?.toIntOrNull()
    val ltiLaunchId = params[LTI_LAUNCH_ID] ?: return null
    val lmsDescription = params[LTI_LMS_DESCRIPTION] ?: return null
    return LtiCourseParams(ltiLaunchId, lmsDescription, studyItem)
  }

  override fun processCourseParams(project: Project, course: Course, context: LtiCourseParams) {
    val ltiSettingsState = LTISettingsManager.getInstance(project).state
    ltiSettingsState.launchId = context.launchId
    ltiSettingsState.lmsDescription = context.lmsDescription
    ltiSettingsState.onlineService = LTIOnlineService.STANDALONE
    val studyItemId = context.studyItemId ?: -1
    StudyItemSelectionService.getInstance(project).setCurrentStudyItem(studyItemId)
  }

  companion object {
    const val LTI_STUDY_ITEM_ID = "study_item_id"
    const val LTI_LAUNCH_ID = "lti_launch_id"
    const val LTI_LMS_DESCRIPTION = "lti_lms_description"
  }
}