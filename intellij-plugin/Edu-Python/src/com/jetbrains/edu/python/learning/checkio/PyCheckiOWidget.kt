package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.profileUrl
import com.jetbrains.edu.python.learning.messages.EduPythonBundle

class PyCheckiOWidget(project: Project) : LoginWidget<CheckiOAccount>(
  project,
  EduPythonBundle.message("checkio.widget.title"),
  EduPythonBundle.message("checkio.widget.tooltip"),
  EducationalCoreIcons.Platform.CheckiO
) {
  override val connector: EduOAuthCodeFlowConnector<CheckiOAccount, *>
    get() = PyCheckiOOAuthConnector

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl

  override fun ID() = "PyCheckiOAccountWidget"

}