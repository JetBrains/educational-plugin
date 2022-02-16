package com.jetbrains.edu.learning.settings

import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import javax.swing.event.HyperlinkEvent

abstract class OAuthLoginOptions <T : OAuthAccount<out Any>> : LoginOptions<T>() {
  protected abstract val connector: EduOAuthConnector<T, *>

  override fun getCurrentAccount(): T? = connector.account

  override fun setCurrentAccount(account: T?) {
    connector.account = account
  }

  override fun createAuthorizeListener(): HyperlinkAdapter =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(e: HyperlinkEvent?) {
        connector.doAuthorize(
          Runnable {
            lastSavedAccount = getCurrentAccount()
            updateLoginLabels()
          },
          authorizationPlace = AuthorizationPlace.SETTINGS
        )
      }
    }

  override fun createLogOutListener(): HyperlinkAdapter =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        lastSavedAccount = null
        connector.doLogout(authorizationPlace = AuthorizationPlace.SETTINGS)
        updateLoginLabels()
      }
    }
}