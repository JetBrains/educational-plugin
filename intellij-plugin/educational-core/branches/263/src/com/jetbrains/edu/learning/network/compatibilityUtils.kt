package com.jetbrains.edu.learning.network

import com.intellij.credentialStore.Credentials
import com.intellij.util.net.ProxyCredentialStore
import com.intellij.util.net.ProxySettings

// BACKCOMPAT: 2026.2 inline it
fun getIdeProxyCredentials(): Credentials? {
  val ideProxyConfiguration = ProxySettings.getInstance().getProxyConfiguration()
  return ProxyCredentialStore.getInstance().getCredentials(ideProxyConfiguration)
}
