package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.profileUrl
import com.jetbrains.edu.javascript.learning.messages.EduJavaScriptBundle
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import icons.EducationalCoreIcons

class JsCheckiOWidget(project: Project) : LoginWidget<CheckiOAccount>(project,
                                                                      EduJavaScriptBundle.message("checkio.widget.title"),
                                                                      EducationalCoreIcons.JSCheckiO) {
  override val account: CheckiOAccount?
    get() = JsCheckiOSettings.getInstance().account

  override val platformName: String
    get() = CheckiONames.JS_CHECKIO

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl

  override fun ID() = "JsCheckiOAccountWidget"

  override fun authorize() {
    JsCheckiOOAuthConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    JsCheckiOSettings.getInstance().account = null
    project.messageBus.syncPublisher(CheckiOOAuthConnector.getAuthorizationTopic()).userLoggedOut()
  }
}