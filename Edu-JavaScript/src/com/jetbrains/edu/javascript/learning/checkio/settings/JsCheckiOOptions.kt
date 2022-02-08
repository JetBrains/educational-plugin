package com.jetbrains.edu.javascript.learning.checkio.settings

import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.profileUrl
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.settings.OAuthLoginOptions

class JsCheckiOOptions : OAuthLoginOptions<CheckiOAccount>() {
  override val connector: EduOAuthConnector<CheckiOAccount, *>
    get() = JsCheckiOOAuthConnector

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl
}