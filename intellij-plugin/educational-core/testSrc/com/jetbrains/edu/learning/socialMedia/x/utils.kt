package com.jetbrains.edu.learning.socialMedia.x

import com.intellij.credentialStore.PasswordSafeSettings
import com.intellij.credentialStore.ProviderType
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.passwordSafe.impl.TestPasswordSafeImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.replaceService
import com.intellij.util.application

fun enableAuth2ForX(disposable: Disposable) {
  val value = Registry.get("edu.socialMedia.x.oauth2")
  val oldValue = value.asBoolean()
  value.setValue(true)
  Disposer.register(disposable) { value.setValue(oldValue) }
}

fun inMemoryPasswordSafe(disposable: Disposable) {
  val passwordSafeSettings = PasswordSafeSettings()
  passwordSafeSettings.providerType = ProviderType.MEMORY_ONLY
  val passwordSafe = TestPasswordSafeImpl(passwordSafeSettings)
  application.replaceService(PasswordSafe::class.java, passwordSafe, disposable)
}

@Suppress("UnusedReceiverParameter")
fun XAccount.Factory.create(): XAccount {
  val tokenExpiresIn = System.currentTimeMillis() + 10000
  return XAccount(XUserInfo("foo", "bar"), tokenExpiresIn)
}
