package com.jetbrains.edu.learning.codeforces.authorization

import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.settings.LoginOptions
import javax.swing.event.HyperlinkEvent

class CodeforcesLonginOptions : LoginOptions<CodeforcesAccount>() {

  override fun getCurrentAccount(): CodeforcesAccount? {
    return CodeforcesSettings.getInstance().account
  }

  override fun setCurrentAccount(account: CodeforcesAccount?) {
    CodeforcesSettings.getInstance().account = account
  }

  override fun profileUrl(account: CodeforcesAccount): String {
    return "${CodeforcesNames.CODEFORCES_URL}/profile/${getCurrentAccount()?.userInfo?.handle}"
  }

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        val account = CodeforcesAuthorizer.login()
        setCurrentAccount(account)
        lastSavedAccount = getCurrentAccount()
        updateLoginLabels()
      }
    }
  }

  override fun getDisplayName(): String {
    return CodeforcesNames.CODEFORCES_TITLE
  }
}
