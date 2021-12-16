package com.jetbrains.edu.javascript.learning.checkio.settings

import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.profileUrl
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.options.CheckiOOptions
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import org.jetbrains.annotations.Nls

class JsCheckiOOptions : CheckiOOptions(JsCheckiOOAuthConnector) {
  @Nls
  override fun getDisplayName(): String = CheckiONames.JS_CHECKIO

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl
}