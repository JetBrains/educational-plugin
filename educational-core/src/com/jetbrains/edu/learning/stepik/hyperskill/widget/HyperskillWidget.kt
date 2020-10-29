package com.jetbrains.edu.learning.stepik.hyperskill.widget

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.SynchronizationStep
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.profileUrl
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.update.SyncHyperskillCourseAction
import icons.EducationalCoreIcons
import javax.swing.Icon

class HyperskillWidget(project: Project) : LoginWidget<HyperskillAccount>(project, HyperskillConnector.AUTHORIZATION_TOPIC, EduNames.JBA) {
  override val account: HyperskillAccount? get() = HyperskillSettings.INSTANCE.account
  override val icon: Icon get() = EducationalCoreIcons.JB_ACADEMY_ENABLED
  override val disabledIcon: Icon get() = EducationalCoreIcons.JB_ACADEMY_DISABLED
  override val syncStep: SynchronizationStep
    get() = SynchronizationStep(EduCoreBundle.message("hyperskill.action.synchronize.project"), SyncHyperskillCourseAction())

  override fun profileUrl(account: HyperskillAccount): String = account.profileUrl

  override fun ID() = "HyperskillAccountWidget"

  override fun authorize() {
    HyperskillConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    HyperskillSettings.INSTANCE.account = null
    project.messageBus.syncPublisher<EduLogInListener>(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()
  }
}
