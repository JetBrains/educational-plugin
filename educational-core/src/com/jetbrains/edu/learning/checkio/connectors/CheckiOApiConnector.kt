package com.jetbrains.edu.learning.checkio.connectors

import com.jetbrains.edu.learning.checkio.api.CheckiOApiInterface
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission

abstract class CheckiOApiConnector(
  private val checkiOApiInterface: CheckiOApiInterface,
  private val oauthConnector: CheckiOOAuthConnector
) {
  abstract val languageId: String

  open fun getMissionList(): List<CheckiOMission> {
    val accessToken = oauthConnector.getAccessToken()
    return checkiOApiInterface.getMissionList(accessToken).execute()
  }
}