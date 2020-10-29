package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.SynchronizationStep
import com.jetbrains.edu.learning.actions.SyncStepikCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import icons.EducationalCoreIcons
import javax.swing.Icon

class StepikWidget(project: Project) : LoginWidget<StepikUser>(project, EduSettings.SETTINGS_CHANGED, StepikNames.STEPIK) {
  override val account: StepikUser?
    get() = EduSettings.getInstance().user
  override val icon: Icon
    get() = EducationalCoreIcons.Stepik
  override val syncStep: SynchronizationStep
    get() = SynchronizationStep(EduCoreBundle.message("action.synchronize.course"), SyncStepikCourseAction())

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