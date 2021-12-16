package com.jetbrains.edu.learning.checkio.options

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector.Companion.authorizationTopic
import com.jetbrains.edu.learning.settings.LoginOptions
import javax.swing.event.HyperlinkEvent

abstract class CheckiOOptions protected constructor(private val myOAuthConnector: CheckiOOAuthConnector) : LoginOptions<CheckiOAccount>() {
  override fun getCurrentAccount(): CheckiOAccount? {
    return myOAuthConnector.account
  }

  override fun setCurrentAccount(account: CheckiOAccount?) {
    myOAuthConnector.account = account
    val messageBus = ApplicationManager.getApplication().messageBus
    if (account != null) {
      messageBus.syncPublisher(authorizationTopic).userLoggedIn()
    }
    else {
      messageBus.syncPublisher(authorizationTopic).userLoggedOut()
    }
  }

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        myOAuthConnector.doAuthorize(Runnable {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        })
      }
    }
  }
}