package com.jetbrains.edu.python.learning.checkio.settings

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.settings.LoginOptions
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.python.learning.checkio.PyCheckiOSettings
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.profileUrl
import org.jetbrains.annotations.Nls
import javax.swing.event.HyperlinkEvent

class PyCheckiOOptions : LoginOptions<CheckiOAccount>() {
  override fun getCurrentAccount(): CheckiOAccount? = PyCheckiOSettings.getInstance().account

  override fun setCurrentAccount(account: CheckiOAccount?) {
    PyCheckiOSettings.getInstance().account = account
    val messageBus = ApplicationManager.getApplication().messageBus
    if (account != null) {
      messageBus.syncPublisher(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedIn()
    }
    else {
      messageBus.syncPublisher(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()
    }
  }

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        PyCheckiOOAuthConnector.doAuthorize(Runnable {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        })
      }
    }
  }

  @Nls
  override fun getDisplayName(): String = CheckiONames.PY_CHECKIO

  override fun profileUrl(account: CheckiOAccount): String = account.profileUrl
}