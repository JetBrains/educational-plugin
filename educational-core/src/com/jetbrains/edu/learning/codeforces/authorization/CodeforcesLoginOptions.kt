package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.actions.CodeforcesLoginAction
import com.jetbrains.edu.learning.settings.LoginOptions
import javax.swing.event.HyperlinkEvent

class CodeforcesLoginOptions : LoginOptions<CodeforcesAccount>() {

  override fun getCurrentAccount(): CodeforcesAccount? = CodeforcesSettings.getInstance().account

  override fun setCurrentAccount(account: CodeforcesAccount?) {
    CodeforcesSettings.getInstance().account = account
  }

  override fun profileUrl(account: CodeforcesAccount): String {
    return "${CodeforcesNames.CODEFORCES_URL}/profile/${getCurrentAccount()?.userInfo?.handle}"
  }

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        val loginAction = ActionManager.getInstance().getAction(CodeforcesLoginAction.ACTION_ID)
        val actionEvent = AnActionEvent.createFromAnAction(loginAction,
                                                           null,
                                                           ActionPlaces.UNKNOWN,
                                                           DataManager.getInstance().getDataContext())

        loginAction.actionPerformed(actionEvent).let {
          if (CodeforcesSettings.getInstance().isLoggedIn()) {
            lastSavedAccount = getCurrentAccount()
            updateLoginLabels()
          }
        }
      }
    }
  }

  override fun getDisplayName(): String {
    return CodeforcesNames.CODEFORCES_TITLE
  }
}
