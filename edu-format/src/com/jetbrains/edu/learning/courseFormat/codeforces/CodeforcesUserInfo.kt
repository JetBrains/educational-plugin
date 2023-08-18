package com.jetbrains.edu.learning.courseFormat.codeforces

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.UserInfo

const val HANDLE = "handle"
const val SESSION_EXPIRES_AT = "sessionExpiresAt"

class CodeforcesUserInfo : UserInfo {
  @JsonProperty(HANDLE)
  var handle: String = ""

  @JsonProperty(SESSION_EXPIRES_AT)
  var sessionExpiresAt: Long = -1

  override var isGuest: Boolean = false

  override fun getFullName(): String {
    return handle
  }

  override fun toString(): String {
    return handle
  }
}