package com.jetbrains.edu.learning.socialMedia.x

import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.UserInfo

class XAccount(userInfo: XUserInfo, tokenExpiresIn: Long) : OAuthAccount<XUserInfo>(userInfo, tokenExpiresIn) {
  override val servicePrefix: String
    get() = XUtils.PLATFORM_NAME

  object Factory
}

data class XUserInfo(
  /**
   * The X handle (screen name) of this user
   */
  val userName: String,
  /**
   * The friendly name of this User, as shown on their profile
   */
  val name: String
) : UserInfo {
  override fun getFullName(): String = userName
}
