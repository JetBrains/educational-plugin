package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.profileUrl
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import icons.EducationalCoreIcons

class PyCheckiOWidget(project: Project) : LoginWidget<CheckiOAccount>(project,
                                                                      EduPythonBundle.message("checkio.widget.title"),
                                                                      EducationalCoreIcons.CheckiO) {
  override val account: CheckiOAccount?
    get() = PyCheckiOSettings.INSTANCE.account

  override val platformName: String
    get() = CheckiONames.PY_CHECKIO

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl

  override fun ID() = "PyCheckiOAccountWidget"

  override fun authorize() {
    PyCheckiOOAuthConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    PyCheckiOSettings.INSTANCE.account = null
    project.messageBus.syncPublisher(CheckiOOAuthConnector.getAuthorizationTopic()).userLoggedOut()
  }
}