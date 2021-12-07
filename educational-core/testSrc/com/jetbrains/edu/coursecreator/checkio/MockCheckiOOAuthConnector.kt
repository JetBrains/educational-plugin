package com.jetbrains.edu.coursecreator.checkio

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector

object MockCheckiOOAuthConnector : CheckiOOAuthConnector() {
  override var account: CheckiOAccount? = null

  override val baseUrl: String = ""

  override val clientId: String = ""

  override val clientSecret: String = ""

  override val oAuthServicePath: String = ""

  override val platformName: String = ""

  override fun getAccessToken(): String = ""
}