package com.jetbrains.edu.learning.stepik.hyperskill.widget

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.actions.SyncCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.profileUrl
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.update.SyncHyperskillCourseAction
import icons.EducationalCoreIcons

class HyperskillWidget(project: Project) : LoginWidget<HyperskillAccount>(project, EduCoreBundle.message("hyperskill.widget.title"),
                                                                          EducationalCoreIcons.JB_ACADEMY_ENABLED) {
  override val account: HyperskillAccount? get() = HyperskillSettings.INSTANCE.account
  override val synchronizeCourseAction: SyncCourseAction
    get() = SyncHyperskillCourseAction()

  override val platformName: String
    get() = EduNames.JBA

  override fun profileUrl(account: HyperskillAccount): String = account.profileUrl

  override fun ID() = "HyperskillAccountWidget"

  override fun authorize() {
    HyperskillConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    HyperskillSettings.INSTANCE.account = null
    project.messageBus.syncPublisher(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()
  }
}
