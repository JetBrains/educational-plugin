package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.NavigationSettings
import com.jetbrains.edu.learning.newproject.CourseParamsProcessor

data class LtiCourseParams(val ltiLaunchId: String, val ltiLmsDescription: String, val studyItemId: Int?)

class LtiCourseParamsProcessor : CourseParamsProcessor<LtiCourseParams> {
  override fun findApplicableContext(params: Map<String, String?>): LtiCourseParams? {
    val studyItem = params[LTI_STUDY_ITEM_ID]?.toIntOrNull()
    val ltiLaunchId = params[LTI_LAUNCH_ID] ?: return null
    val lmsDescription = params[LTI_LMS_DESCRIPTION] ?: return null
    return LtiCourseParams(ltiLaunchId, lmsDescription, studyItem)
  }

  override fun processCourseParams(project: Project, course: Course, params: LtiCourseParams) {
    val ltiSettingsState = LTISettingsManager.instance(project).state
    ltiSettingsState.launchId = params.ltiLaunchId
    ltiSettingsState.lmsDescription = params.ltiLmsDescription
    ltiSettingsState.onlineService = LTIOnlineService.STANDALONE
    val studyItemId = params.studyItemId ?: -1
    NavigationSettings.getInstance(project).setCurrentStudyItem(studyItemId)
  }

  companion object {
    const val LTI_STUDY_ITEM_ID = "study_item_id"
    const val LTI_LAUNCH_ID = "lti_launch_id"
    const val LTI_LMS_DESCRIPTION = "lti_lms_description"
  }
}