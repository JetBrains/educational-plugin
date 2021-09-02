package com.jetbrains.edu.learning.authUtils

import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

abstract class DataFormAccount<UserInfo : Any> : Account<UserInfo> {

  private val serviceNameForSessionId @NlsSafe get() = "$serviceName session id"
  var sessionCookieExpiresAt: Long = -1

  constructor()

  constructor(sessionCookieExpiresAt: Long) {
    this.sessionCookieExpiresAt = sessionCookieExpiresAt
  }

  override fun isUpToDate(): Boolean = System.currentTimeMillis() < sessionCookieExpiresAt

  fun getSessionId(): String? {
    return getToken(getUserName(), serviceNameForSessionId)
  }

  fun saveSessionId(sessionId: String) {
    val userName = getUserName()
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForSessionId), Credentials(userName, sessionId))
  }
}

fun <Account : DataFormAccount<UserInfo>, UserInfo : Any> deserializeDataFormAccount(
  xmlAccount: Element,
  accountClass: Class<Account>,
  userInfoClass: Class<UserInfo>): Account {

  val account = XmlSerializer.deserialize(xmlAccount, accountClass)

  val userInfo = ReflectionUtil.newInstance(userInfoClass)
  XmlSerializer.deserializeInto(userInfo, xmlAccount)
  account.userInfo = userInfo

  return account
}