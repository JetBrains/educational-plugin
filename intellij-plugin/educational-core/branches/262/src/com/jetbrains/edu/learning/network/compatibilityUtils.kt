package com.jetbrains.edu.learning.network

import com.intellij.credentialStore.Credentials
import com.intellij.util.net.ProxyCredentialStore
import com.intellij.util.net.ProxySettings
import com.intellij.util.net.asProxyCredentialProvider
import com.intellij.util.net.getStaticProxyCredentials

fun getIdeProxyCredentials(): Credentials? {
  val provider = ProxyCredentialStore.getInstance().asProxyCredentialProvider()
  return ProxySettings.getInstance().getStaticProxyCredentials(provider)
}
