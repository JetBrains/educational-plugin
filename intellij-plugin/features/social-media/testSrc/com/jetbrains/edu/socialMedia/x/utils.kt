package com.jetbrains.edu.socialMedia.x

import com.intellij.credentialStore.PasswordSafeSettings
import com.intellij.credentialStore.ProviderType
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.passwordSafe.impl.TestPasswordSafeImpl
import com.intellij.openapi.Disposable
import com.intellij.testFramework.replaceService
import com.intellij.util.application

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
