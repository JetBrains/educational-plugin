package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import icons.EducationalCoreIcons
import javax.swing.Icon

class JsCheckiOWidget(project: Project) : LoginWidget(project, CheckiOOAuthConnector.getAuthorizationTopic(), JsCheckiONames.JS_CHECKIO) {
  override val account: OAuthAccount<out Any>?
    get() = JsCheckiOSettings.getInstance().account
  override val icon: Icon
    get() = EducationalCoreIcons.JSCheckiO

  override fun ID() = "JsCheckiOAccountWidget"

  override fun authorize() {
    JsCheckiOOAuthConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    JsCheckiOSettings.getInstance().account = null
    project.messageBus.syncPublisher<EduLogInListener>(
      CheckiOOAuthConnector.getAuthorizationTopic()).userLoggedOut()
  }
}