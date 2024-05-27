package com.jetbrains.edu.learning.authUtils

import com.intellij.credentialStore.ACCESS_TO_KEY_CHAIN_DENIED
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.jetbrains.edu.learning.authUtils.OAuthUtils.credentialAttributes
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager

class PasswordServiceImpl : PasswordService {
  override fun getSecret(userName: String, serviceNameForPasswordSafe: String): String? {
    val credentials = PasswordSafe.instance.get(credentialAttributes(userName, serviceNameForPasswordSafe)) ?: return null
    if (credentials == ACCESS_TO_KEY_CHAIN_DENIED) {
      EduNotificationManager.showErrorNotification(
        title = EduCoreBundle.message("notification.tokens.access.denied.title", userName),
        content = EduCoreBundle.message("notification.tokens.access.denied.text")
      )
      return null
    }
    return credentials.getPasswordAsString()
  }

  override fun saveSecret(userName: String, serviceNameForPasswordSafe: String, secret: String) {
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForPasswordSafe), Credentials(userName, secret))
  }
}
