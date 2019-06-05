package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.settings.OauthOptions
import org.jetbrains.annotations.Nls
import javax.swing.event.HyperlinkEvent

class HyperskillOptions : OauthOptions<HyperskillAccount>() {
  init {
    initAccounts()
  }

  override fun getCurrentAccount() : HyperskillAccount? = HyperskillSettings.INSTANCE.account

  override fun isAvailable(): Boolean = isHyperskillSupportAvailable()

  override fun setCurrentAccount(lastSavedAccount: HyperskillAccount?) {
    HyperskillSettings.INSTANCE.account = lastSavedAccount
    val messageBus = ApplicationManager.getApplication().messageBus
    if (lastSavedAccount != null) {
      messageBus.syncPublisher<EduLogInListener>(HyperskillConnector.hyperskillAuthorizationTopic).userLoggedIn()
    }
    else {
      messageBus.syncPublisher<EduLogInListener>(HyperskillConnector.hyperskillAuthorizationTopic).userLoggedOut()
    }
  }

  @Nls
  override fun getDisplayName(): String {
    return HYPERSKILL
  }

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        HyperskillConnector.doAuthorize(Runnable {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        })
      }
    }
  }
}
