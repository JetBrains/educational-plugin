package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.SynchronizationStep
import com.jetbrains.edu.learning.actions.SyncStepikCourseAction
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import icons.EducationalCoreIcons
import javax.swing.Icon

class StepikWidget(project: Project) : LoginWidget(project, EduSettings.SETTINGS_CHANGED) {
  override val account: OAuthAccount<out Any>?
    get() = EduSettings.getInstance().user
  override val linkName: String
    get() = StepikNames.STEPIK
  override val icon: Icon
    get() = EducationalCoreIcons.Stepik
  override val syncStep: SynchronizationStep
    get() = SynchronizationStep("Synchronize course", SyncStepikCourseAction())

  override fun ID() = ID

  override fun authorize() {
    EduUsagesCollector.loginFromWidget()
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
  }

  override fun resetAccount() {
    EduUsagesCollector.logoutFromWidget()
    EduSettings.getInstance().user = null
  }

  companion object {
    const val ID = "StepikWidget"
  }
}