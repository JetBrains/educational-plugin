package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.hyperskillAuthorizationTopic
import icons.EducationalCoreIcons
import javax.swing.Icon

class HyperskillWidget(project: Project) : LoginWidget(project, hyperskillAuthorizationTopic) {
  override val account: OAuthAccount<out Any>?
    get() = HyperskillSettings.INSTANCE.account
  override val linkName: String
    get() = HYPERSKILL
  override val icon: Icon
    get() = EducationalCoreIcons.Hyperskill

  override fun ID() = "HyperskillAccountWidget"

  override fun authorize() {
    HyperskillConnector.doAuthorize()
  }

  override fun resetAccount() {
    HyperskillSettings.INSTANCE.account = null
    project.messageBus.syncPublisher<EduLogInListener>(hyperskillAuthorizationTopic).userLoggedOut()
  }
}
