package com.jetbrains.edu.learning.settings

import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.authUtils.EduLoginConnector
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.UserInfo
import javax.swing.event.HyperlinkEvent

abstract class OAuthLoginOptions <T : OAuthAccount<out UserInfo>> : LoginOptions<T>() {
  protected abstract val connector: EduLoginConnector<T, *>

  override fun getCurrentAccount(): T? = connector.account

  override fun setCurrentAccount(account: T?) {
    connector.account = account
  }

  override fun createAuthorizeListener(): HyperlinkAdapter =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(e: HyperlinkEvent) {
        connector.doAuthorize(Runnable { postLoginActions() }, authorizationPlace = AuthorizationPlace.SETTINGS)
      }
    }

  open fun postLoginActions() {
    lastSavedAccount = getCurrentAccount()
    updateLoginLabels()
  }

  override fun createLogOutListener(): HyperlinkAdapter? =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        val currentConnector = connector
        if (currentConnector is EduOAuthCodeFlowConnector) {
          lastSavedAccount = null
          currentConnector.doLogout(authorizationPlace = AuthorizationPlace.SETTINGS)
          updateLoginLabels()
        }
      }
    }
}