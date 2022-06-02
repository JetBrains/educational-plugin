package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.settings.LoginOptions
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import javax.swing.event.HyperlinkEvent

class CodeforcesLoginOptions : LoginOptions<CodeforcesAccount>() {

  override fun getCurrentAccount(): CodeforcesAccount? = CodeforcesSettings.getInstance().account

  override fun setCurrentAccount(account: CodeforcesAccount?) {
   CodeforcesSettings.getInstance().account = account
  }

  override fun profileUrl(account: CodeforcesAccount): String {
    return "${CodeforcesNames.CODEFORCES_URL}/profile/${getCurrentAccount()?.userInfo?.handle}"
  }

  override fun createAuthorizeListener(): HyperlinkAdapter =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(e: HyperlinkEvent) {
        if (LoginDialog(AuthorizationPlace.SETTINGS).showAndGet()) {
          if (CodeforcesSettings.getInstance().isLoggedIn()) {
            lastSavedAccount = getCurrentAccount()
            updateLoginLabels()
          }
        }
      }
    }

  override fun createLogOutListener(): HyperlinkAdapter =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        lastSavedAccount = null
        CodeforcesSettings.getInstance().setAccountWithStatisticsEvent(null, AuthorizationPlace.SETTINGS)
        updateLoginLabels()
      }
    }

  override fun getDisplayName(): String {
    return CodeforcesNames.CODEFORCES_TITLE
  }
}
