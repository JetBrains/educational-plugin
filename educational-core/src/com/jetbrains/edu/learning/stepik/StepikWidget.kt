package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.actions.SyncCourseAction
import com.jetbrains.edu.learning.actions.SyncStepikCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import icons.EducationalCoreIcons

class StepikWidget(project: Project) : LoginWidget<StepikUser>(project,
                                                               EduCoreBundle.message("stepik.widget.title"),
                                                               EducationalCoreIcons.Stepik) {
  override val account: StepikUser?
    get() = EduSettings.getInstance().user

  override val synchronizeCourseAction: SyncCourseAction
    get() = SyncStepikCourseAction()

  override val platformName: String
    get() = StepikNames.STEPIK

  override fun profileUrl(account: StepikUser): String = account.profileUrl

  override fun ID() = ID

  override fun authorize() {
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
  }

  override fun resetAccount() {
    EduSettings.getInstance().user = null
  }

  companion object {
    const val ID = "StepikWidget"
  }
}