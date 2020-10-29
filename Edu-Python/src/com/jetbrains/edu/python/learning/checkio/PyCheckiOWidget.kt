package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.profileUrl
import icons.EducationalCoreIcons
import javax.swing.Icon

class PyCheckiOWidget(project: Project) : LoginWidget<CheckiOAccount>(project, CheckiOOAuthConnector.getAuthorizationTopic(),
                                                                      CheckiONames.PY_CHECKIO) {
  override val account: CheckiOAccount?
    get() = PyCheckiOSettings.INSTANCE.account
  override val icon: Icon
    get() = EducationalCoreIcons.CheckiO

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl

  override fun ID() = "PyCheckiOAccountWidget"

  override fun authorize() {
    PyCheckiOOAuthConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    PyCheckiOSettings.INSTANCE.account = null
    project.messageBus.syncPublisher<EduLogInListener>(
      CheckiOOAuthConnector.getAuthorizationTopic()).userLoggedOut()
  }
}