package com.jetbrains.edu.javascript.learning.checkio.connectors

import com.jetbrains.edu.javascript.learning.checkio.JsCheckiOSettings
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiOOAuthBundle
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiONames

object JsCheckiOOAuthConnector : CheckiOOAuthConnector() {
  override var account: CheckiOAccount?
    get() = JsCheckiOSettings.getInstance().account
    set(account) {
      JsCheckiOSettings.getInstance().account = account
    }

  override val clientId: String = JsCheckiOOAuthBundle.value("jsCheckioClientId")

  override val clientSecret: String = JsCheckiOOAuthBundle.value("jsCheckioClientSecret")

  override val oAuthServicePath: String = JsCheckiONames.JS_CHECKIO_OAUTH_SERVICE_PATH

  override val platformName: String = CheckiONames.JS_CHECKIO
}