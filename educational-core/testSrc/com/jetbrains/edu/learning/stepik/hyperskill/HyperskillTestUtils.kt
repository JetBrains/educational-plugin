package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillUserInfo
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

fun loginFakeUser() {
  val fakeToken = TokenInfo().apply { accessToken = "faketoken" }
  HyperskillSettings.INSTANCE.account = HyperskillAccount().apply {
    userInfo = HyperskillUserInfo()
    userInfo.id = 1
    tokenInfo = fakeToken
  }
}