package com.jetbrains.edu.learning.checkio.account

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.UserInfo

class CheckiOUserInfo : UserInfo {
  @JsonProperty("username")
  var username = ""

  @JsonProperty("uid")
  var uid: Int = -1

  override fun getFullName(): String = username

  override fun equals(other: Any?): Boolean {
    if (other == null || other.javaClass != javaClass) {
      return false
    }
    return uid == (other as CheckiOUserInfo).uid
  }

  override fun hashCode(): Int = uid

  override fun toString(): String = username

  override var isGuest: Boolean
    get() = false
    set(_) {}
}
