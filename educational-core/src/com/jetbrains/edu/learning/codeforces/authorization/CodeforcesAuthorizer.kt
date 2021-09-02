package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.onError
import org.jetbrains.projector.common.misc.firstNotNullOrNull
import org.jsoup.Jsoup
import java.time.Instant
import java.time.temporal.ChronoUnit

object CodeforcesAuthorizer {

  private val handleRegex = """var handle = "([\w\-]+)"""".toRegex()

  fun login(): CodeforcesAccount? {
    val loginDialog = LoginDialog()
    if (loginDialog.showAndGet()) {
      val credentials = loginDialog.credentials!!
      val userName = credentials.userName!!
      val password = credentials.password!!.toString()

      val (token, jSessionId) = CodeforcesConnector.getInstance().getCSRFTokenWithJSessionID().onError {
        Messages.showErrorDialog(it, "Login Error")
        return null
      }
      val loginResponse = CodeforcesConnector.getInstance().postLoginForm(userName, password, jSessionId, token).onError {
        Messages.showErrorDialog(it, "Login Error")
        return null
      }

      val string = loginResponse.body()!!.string()


      if (string.contains("Invalid handle/email or password")) {
        Messages.showErrorDialog("Invalid handle/email or password", "Login Error")
        return null
      }

      if (loginResponse.isSuccessful) {
        var handle = Jsoup.parse(string)
          .getElementsByTag("script")
          .map { it.data() }
          .firstNotNullOrNull { handleRegex.find(it) }
          ?.destructured?.toList()?.firstOrNull()
        if (handle == null) handle = CodeforcesConnector.getInstance().getProfile(jSessionId) ?: return null
        val account = CodeforcesAccount(Instant.now().plus(27, ChronoUnit.DAYS).toEpochMilli())
        account.userInfo = CodeforcesUserInfo()
        account.userInfo.handle = handle
        account.saveSessionId(jSessionId)
        val credentialAttributes = credentialAttributes(handle)
        PasswordSafe.instance.set(credentialAttributes, Credentials(handle, password))
        return account
      }
      return null
    }
    return null
  }

  private fun credentialAttributes(userName: String): CredentialAttributes {
    return CredentialAttributes(generateServiceName(CodeforcesNames.CODEFORCES_SUBSYSTEM_NAME, userName))
  }
}