package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.authUtils.Account
import com.jetbrains.edu.learning.authUtils.OAuthUtils.credentialAttributes
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TITLE
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesUserInfo
import org.jetbrains.annotations.TestOnly
import java.time.Instant
import java.time.temporal.ChronoUnit

class CodeforcesAccount : Account<CodeforcesUserInfo> {

  private val serviceNameForSessionId @NlsSafe get() = "$serviceName session id"
  private val serviceNameForPassword @NlsSafe get() = "$serviceName password"
  override val servicePrefix: String = CODEFORCES_TITLE

  @TestOnly
  constructor() : super()

  constructor(userInfo: CodeforcesUserInfo) {
    this.userInfo = userInfo
  }

  override fun isUpToDate(): Boolean = System.currentTimeMillis() < userInfo.sessionExpiresAt

  fun getSessionId(): String? {
    return getSecret(getUserName(), serviceNameForSessionId)
  }

  fun getPassword(): String? {
    return getSecret(getUserName(), serviceNameForPassword)
  }

  fun saveSessionId(sessionId: String) {
    val userName = getUserName()
    PasswordSafe.instance.set(getSessionIdCredentialAttributes(), Credentials(userName, sessionId))
    updateExpiresAt()
  }

  fun savePassword(password: String) {
    val userName = getUserName()
    PasswordSafe.instance.set(getPasswordCredentialAttributes(), Credentials(userName, password))
  }

  private fun getSessionIdCredentialAttributes() = credentialAttributes(getUserName(), serviceNameForSessionId)
  private fun getPasswordCredentialAttributes() = credentialAttributes(getUserName(), serviceNameForPassword)

  fun updateExpiresAt() {
    this.userInfo.sessionExpiresAt = Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli()
  }
}

