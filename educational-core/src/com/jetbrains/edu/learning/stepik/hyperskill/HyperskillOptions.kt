package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.settings.OauthOptions
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
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
      messageBus.syncPublisher<EduLogInListener>(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedIn()
    }
    else {
      messageBus.syncPublisher<EduLogInListener>(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()
    }
  }

  @Nls
  override fun getDisplayName(): String {
    return HYPERSKILL
  }

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        HyperskillConnector.getInstance().doAuthorize(Runnable {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        })
      }
    }
  }
}
