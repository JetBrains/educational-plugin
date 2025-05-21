package com.jetbrains.edu.commandLine.processors

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.courseGeneration.openStudyItem
import com.jetbrains.edu.learning.marketplace.lti.LTIOnlineService
import com.jetbrains.edu.learning.marketplace.lti.LTISettingsManager

data class LtiCourseParams(val ltiLaunchId: String, val ltiLmsDescription: String, val studyItemId: Int?)

class LtiCourseParamsProcessor : CourseParamsProcessor<LtiCourseParams> {
  override fun shouldApply(project: Project, course: Course, params: Map<String, String?>): LtiCourseParams? {
    val studyItem = params[LTI_STUDY_ITEM_ID]?.toIntOrNull()
    val ltiLaunchId = params[LTI_LAUNCH_ID] ?: return null
    val lmsDescription = params[LTI_LMS_DESCRIPTION] ?: return null
    return LtiCourseParams(ltiLaunchId, lmsDescription, studyItem)
  }

  override fun processCourseParams(project: Project, course: Course, params: LtiCourseParams): Boolean {
    if (params.studyItemId != null) {
      invokeLater {
        openStudyItem(params.studyItemId, project)
      }
    }
    val settingsState = LTISettingsManager.instance(project).state
    settingsState.launchId = params.ltiLaunchId
    settingsState.lmsDescription = params.ltiLmsDescription
    settingsState.onlineService = LTIOnlineService.STANDALONE
    return true
  }

  companion object {
    const val LTI_STUDY_ITEM_ID = "study_item_id"
    const val LTI_LAUNCH_ID = "lti_launch_id"
    const val LTI_LMS_DESCRIPTION = "lti_lms_description"
  }
}