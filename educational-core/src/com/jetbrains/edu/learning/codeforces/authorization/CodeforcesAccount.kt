package com.jetbrains.edu.learning.codeforces.authorization

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.authUtils.DataFormAccount
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TITLE
import org.jetbrains.annotations.TestOnly

const val HANDLE = "handle"

class CodeforcesAccount : DataFormAccount<CodeforcesUserInfo> {
  @TestOnly
  constructor() : super()

  constructor(sessionCookieExpiresAs: Long) : super(sessionCookieExpiresAs)

  override val servicePrefix: String = CODEFORCES_TITLE

  override fun getUserName(): String {
    return userInfo.getFullName()
  }

}

class CodeforcesUserInfo : UserInfo {
  @JsonProperty(HANDLE)
  var handle: String = ""

  override fun getFullName(): String {
    return handle
  }

  override fun toString(): String {
    return handle
  }
}