package com.jetbrains.edu.python.learning.checkio.connectors

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.python.learning.checkio.PyCheckiOSettings
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiOOAuthBundle

private val CLIENT_ID: String = PyCheckiOOAuthBundle.value("pyCheckioClientId")
private val CLIENT_SECRET: String = PyCheckiOOAuthBundle.value("pyCheckioClientSecret")

object PyCheckiOOAuthConnector : CheckiOOAuthConnector(CLIENT_ID, CLIENT_SECRET) {
  override var account: CheckiOAccount?
    get() = PyCheckiOSettings.INSTANCE.account
    set(account) {
      PyCheckiOSettings.INSTANCE.account = account
    }

  override val oAuthServicePath: String = PyCheckiONames.PY_CHECKIO_OAUTH_SERVICE_PATH

  override val platformName: String = CheckiONames.PY_CHECKIO
}