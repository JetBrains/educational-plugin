package com.jetbrains.edu.javascript.learning.checkio.settings

import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.profileUrl
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.settings.OAuthLoginOptions

class JsCheckiOOptions : OAuthLoginOptions<CheckiOAccount>() {
  override val connector: EduOAuthCodeFlowConnector<CheckiOAccount, *>
    get() = JsCheckiOOAuthConnector

  override fun getDisplayName(): String = CheckiONames.JS_CHECKIO

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl
}