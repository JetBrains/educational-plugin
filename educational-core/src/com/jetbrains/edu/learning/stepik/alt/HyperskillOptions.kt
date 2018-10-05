package com.jetbrains.edu.learning.stepik.alt

import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.learning.settings.OauthOptions
import org.jetbrains.annotations.Nls
import javax.swing.event.HyperlinkEvent

class HyperskillOptions : OauthOptions<HyperskillAccount>() {
  init {
    initAccounts()
  }

  override fun getCurrentAccount() : HyperskillAccount? = HyperskillSettings.instance.account

  override fun setCurrentAccount(lastSavedAccount: HyperskillAccount?) {
    HyperskillSettings.instance.account = lastSavedAccount
  }

  @Nls
  override fun getDisplayName(): String {
    return "Hyperskill"
  }

  override fun createAuthorizeListener(): HyperlinkAdapter {
    return object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        HyperskillConnector.doAuthorize(Runnable {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        })
      }
    }
  }
}
