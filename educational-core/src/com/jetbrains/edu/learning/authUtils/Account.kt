package com.jetbrains.edu.learning.authUtils

import com.intellij.credentialStore.ACCESS_TO_KEY_CHAIN_DENIED
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jdom.Element

private const val SERVICE_DISPLAY_NAME_PREFIX = "EduTools"

abstract class Account<UserInfo : Any> {
  protected abstract val servicePrefix: String
  protected val serviceName get() = "$servicePrefix Integration"

  @field:Transient
  @get:Transient
  lateinit var userInfo: UserInfo

  abstract fun isUpToDate(): Boolean
  protected abstract fun getUserName(): String

  fun serialize(): Element? {
    if (PasswordSafe.instance.isMemoryOnly) {
      return null
    }
    val accountElement = XmlSerializer.serialize(this, SkipDefaultValuesSerializationFilters())

    XmlSerializer.serializeInto(userInfo, accountElement)

    return accountElement
  }

  protected fun getSecret(userName: String?, serviceNameForPasswordSafe: String?): String? {
    userName ?: return null
    serviceNameForPasswordSafe ?: return null
    val credentials = PasswordSafe.instance.get(credentialAttributes(userName, serviceNameForPasswordSafe)) ?: return null
    if (credentials == ACCESS_TO_KEY_CHAIN_DENIED) {
      val notification = Notification("EduTools", EduCoreBundle.message("notification.tokens.access.denied.title", userName),
                                      EduCoreBundle.message("notification.tokens.access.denied.text"), NotificationType.ERROR)
      notification.notify(null)
      return null
    }
    return credentials.getPasswordAsString()
  }

  protected fun credentialAttributes(userName: String, serviceName: String) =
    CredentialAttributes(generateServiceName("${SERVICE_DISPLAY_NAME_PREFIX} $serviceName", userName))
}

fun <UserAccount : Account<UserInfo>, UserInfo : Any> deserializeAccount(
  xmlAccount: Element,
  accountClass: Class<UserAccount>,
  userInfoClass: Class<UserInfo>): UserAccount {

  val account = XmlSerializer.deserialize(xmlAccount, accountClass)

  val userInfo = ReflectionUtil.newInstance(userInfoClass)
  XmlSerializer.deserializeInto(userInfo, xmlAccount)
  account.userInfo = userInfo

  return account
}
