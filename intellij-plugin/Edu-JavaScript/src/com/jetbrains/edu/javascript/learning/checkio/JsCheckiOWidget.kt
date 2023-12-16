package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.profileUrl
import com.jetbrains.edu.javascript.learning.messages.EduJavaScriptBundle
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount

class JsCheckiOWidget(project: Project) : LoginWidget<CheckiOAccount>(project,
                                                                      EduJavaScriptBundle.message("checkio.widget.title"),
                                                                      EduJavaScriptBundle.message("checkio.widget.tooltip"),
                                                                      EducationalCoreIcons.JSCheckiO) {
  override val connector: EduOAuthCodeFlowConnector<CheckiOAccount, *>
    get() = JsCheckiOOAuthConnector

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl

  override fun ID() = "JsCheckiOAccountWidget"

}