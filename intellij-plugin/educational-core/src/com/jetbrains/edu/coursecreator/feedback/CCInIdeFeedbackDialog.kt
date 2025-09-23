package com.jetbrains.edu.coursecreator.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.jetbrains.edu.learning.feedback.CourseFeedbackInfoData
import com.jetbrains.edu.learning.feedback.InIdeFeedbackDialog

class CCInIdeFeedbackDialog(
  private val courseFeedbackInfoData: CourseFeedbackInfoData
) : InIdeFeedbackDialog<JbAcademyCCFeedbackSystemInfoData>(false, null) {

  override fun computeSystemInfoDataInternal(): JbAcademyCCFeedbackSystemInfoData {
    return createJbAcademyFeedbackSystemInfoData(courseFeedbackInfoData)
  }

  init {
    init()
  }

  override fun showJbAcademyFeedbackSystemInfoDialog(project: Project?, systemInfoData: JbAcademyCCFeedbackSystemInfoData) =
    showSystemInfoDialog(project, systemInfoData) {}
}

private fun createJbAcademyFeedbackSystemInfoData(courseFeedbackInfoData: CourseFeedbackInfoData): JbAcademyCCFeedbackSystemInfoData {
  return JbAcademyCCFeedbackSystemInfoData(CommonFeedbackSystemData.getCurrentData(), courseFeedbackInfoData)
}