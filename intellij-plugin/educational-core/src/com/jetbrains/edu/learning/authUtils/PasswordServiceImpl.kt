package com.jetbrains.edu.learning.authUtils

import com.intellij.credentialStore.ACCESS_TO_KEY_CHAIN_DENIED
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.jetbrains.edu.learning.authUtils.OAuthUtils.credentialAttributes
import com.jetbrains.edu.learning.messages.EduCoreBundle

class PasswordServiceImpl : PasswordService {
  override fun getSecret(userName: String, serviceNameForPasswordSafe: String): String? {
    val credentials = PasswordSafe.instance.get(credentialAttributes(userName, serviceNameForPasswordSafe)) ?: return null
    if (credentials == ACCESS_TO_KEY_CHAIN_DENIED) {
      val notification = Notification(
        "JetBrains Academy", EduCoreBundle.message("notification.tokens.access.denied.title", userName),
        EduCoreBundle.message("notification.tokens.access.denied.text"), NotificationType.ERROR
      )
      notification.notify(null)
      return null
    }
    return credentials.getPasswordAsString()
  }

  override fun saveSecret(userName: String, serviceNameForPasswordSafe: String, secret: String) {
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForPasswordSafe), Credentials(userName, secret))
  }
}
