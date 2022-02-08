package com.jetbrains.edu.python.learning.checkio.settings

import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.settings.OAuthLoginOptions
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.profileUrl

class PyCheckiOOptions : OAuthLoginOptions<CheckiOAccount>() {
  override val connector: EduOAuthConnector<CheckiOAccount, *>
    get() = PyCheckiOOAuthConnector

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl
}