package com.jetbrains.edu.learning.codeforces.authorization

import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.settings.LoginOptions
import javax.swing.event.HyperlinkEvent

class CodeforcesLoginOptions : LoginOptions<CodeforcesAccount>() {

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
        val loginDialog = LoginDialog()
        if (loginDialog.showAndGet()) {
          if (CodeforcesConnector.getInstance().login(loginDialog.loginField.text, String(loginDialog.passwordField.password))) {
            lastSavedAccount = getCurrentAccount()
            updateLoginLabels()
          } else {
            authorize(e)
          }
        }
      }
    }
  }

  override fun getDisplayName(): String {
    return CodeforcesNames.CODEFORCES_TITLE
  }
}
