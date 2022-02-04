package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.actions.SyncStepikCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikConnector

class StepikWidget(project: Project) : LoginWidget<StepikUser>(project,
                                                               EduCoreBundle.message("stepik.widget.title"),
                                                               EduCoreBundle.message("stepik.widget.tooltip"),
                                                               EducationalCoreIcons.Stepik) {
  override val account: StepikUser?
    get() = EduSettings.getInstance().user

  override val synchronizeCourseActionId: String
    get() = SyncStepikCourseAction.ACTION_ID

  override val platformName: String
    get() = StepikNames.STEPIK

  override fun profileUrl(account: StepikUser): String = account.profileUrl

  override fun ID() = ID

  override fun authorize() = StepikConnector.getInstance().doAuthorize()

  override fun resetAccount() {
    EduSettings.getInstance().user = null
  }

  companion object {
    const val ID = "StepikWidget"
  }
}