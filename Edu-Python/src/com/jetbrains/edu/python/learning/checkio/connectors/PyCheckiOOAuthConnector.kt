package com.jetbrains.edu.python.learning.checkio.connectors

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.PY_CHECKIO_PREFIX
import com.jetbrains.edu.python.learning.checkio.PyCheckiOSettings
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiOOAuthBundle

object PyCheckiOOAuthConnector : CheckiOOAuthConnector() {
  override var account: CheckiOAccount?
    get() = PyCheckiOSettings.getInstance().account
    set(account) {
      PyCheckiOSettings.getInstance().account = account
    }

  override val clientId: String = PyCheckiOOAuthBundle.value("pyCheckioClientId")

  override val clientSecret: String = PyCheckiOOAuthBundle.value("pyCheckioClientSecret")

  override val serviceName: String = getCheckiOServiceName(PY_CHECKIO_PREFIX.lowercase())

  override val platformName: String = CheckiONames.PY_CHECKIO
}