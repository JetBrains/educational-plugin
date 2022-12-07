package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.actions.SyncStepikCourseAction
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikConnector

class StepikWidget(project: Project) : LoginWidget<StepikUser>(project,
                                                               EduCoreBundle.message("stepik.widget.title"),
                                                               EduCoreBundle.message("stepik.widget.tooltip"),
                                                               EducationalCoreIcons.Stepik) {
  override val connector: EduOAuthCodeFlowConnector<StepikUser, *>
    get() = StepikConnector.getInstance()

  override val synchronizeCourseActionId: String
    get() = SyncStepikCourseAction.ACTION_ID

  override fun profileUrl(account: StepikUser): String = account.profileUrl

  override fun ID() = ID

  companion object {
    const val ID = "StepikWidget"
  }
}