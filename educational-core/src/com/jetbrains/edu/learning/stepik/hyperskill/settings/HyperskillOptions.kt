package com.jetbrains.edu.learning.stepik.hyperskill.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.settings.OauthOptions
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROFILE_PATH
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillUserInfo
import com.jetbrains.edu.learning.stepik.hyperskill.isHyperskillSupportAvailable
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
    return EduNames.JBA
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

  override fun getProfileUrl(userInfo: Any): String {
    val userId = try {
      (userInfo as HyperskillUserInfo).id
    }
    catch (e: ClassCastException) {
      Logger.getInstance(HyperskillOptions::class.java).error(e.message)
      ""
    }

    return "${HYPERSKILL_PROFILE_PATH}${userId}"
  }
}
