package com.jetbrains.edu.learning.codeforces.authorization

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.authUtils.Account
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TITLE
import org.jdom.Element
import org.jetbrains.annotations.TestOnly
import java.time.Instant
import java.time.temporal.ChronoUnit

const val HANDLE = "handle"

class CodeforcesAccount : Account<CodeforcesUserInfo> {

  private val serviceNameForSessionId @NlsSafe get() = "$serviceName session id"
  private var sessionCookieExpiresAt: Long = -1
  override val servicePrefix: String = CODEFORCES_TITLE

  @TestOnly
  constructor() : super()

  constructor(userInfo: CodeforcesUserInfo) {
    updateExpiresAt()
    this.userInfo = userInfo
  }

  override fun isUpToDate(): Boolean = System.currentTimeMillis() < sessionCookieExpiresAt

  fun getSessionId(): String? {
    return getToken(getUserName(), serviceNameForSessionId)
  }

  fun saveSessionId(sessionId: String) {
    val userName = getUserName()
    PasswordSafe.instance.set(getCredentialAttributes(), Credentials(userName, sessionId))
  }

  override fun getUserName(): String {
    return userInfo.getFullName()
  }

  fun getCredentialAttributes() = credentialAttributes(getUserName(), serviceNameForSessionId)

  fun updateExpiresAt() {
    this.sessionCookieExpiresAt = Instant.now().plus(27, ChronoUnit.DAYS).toEpochMilli()
  }
}

fun deserializeCodeforcesAccount(xmlAccount: Element): CodeforcesAccount {
  val account = XmlSerializer.deserialize(xmlAccount, CodeforcesAccount::class.java)
  val userInfo = ReflectionUtil.newInstance(CodeforcesUserInfo::class.java)
  XmlSerializer.deserializeInto(userInfo, xmlAccount)
  account.userInfo = userInfo

  return account
}

class CodeforcesUserInfo : UserInfo {
  @JsonProperty(HANDLE)
  var handle: String = ""

  override fun getFullName(): String {
    return handle
  }

  override fun toString(): String {
    return handle
  }
}