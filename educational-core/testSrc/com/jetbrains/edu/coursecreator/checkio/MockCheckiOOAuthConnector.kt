package com.jetbrains.edu.coursecreator.checkio

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector

object MockCheckiOOAuthConnector : CheckiOOAuthConnector() {
  @get:Synchronized
  @set:Synchronized
  override var account: CheckiOAccount? = null

  override val baseUrl: String = ""

  override val clientId: String = ""

  override val clientSecret: String = ""

  override val platformName: String = ""

  override fun getAccessToken(): String = ""
}