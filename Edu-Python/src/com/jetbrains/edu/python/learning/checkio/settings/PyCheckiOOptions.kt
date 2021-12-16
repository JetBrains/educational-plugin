package com.jetbrains.edu.python.learning.checkio.settings

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.options.CheckiOOptions
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.profileUrl
import org.jetbrains.annotations.Nls

class PyCheckiOOptions : CheckiOOptions(PyCheckiOOAuthConnector) {
  @Nls
  override fun getDisplayName(): String = CheckiONames.PY_CHECKIO

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl
}