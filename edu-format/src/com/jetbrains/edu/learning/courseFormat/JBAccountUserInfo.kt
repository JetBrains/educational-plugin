package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonProperty

private const val EMAIL = "email"
private const val FULL_NAME = "full_name"
private const val JBA_LOGIN = "jba_login"

class JBAccountUserInfo() : UserInfo {

  @JsonProperty(EMAIL)
  var email: String = ""

  @JsonProperty(FULL_NAME)
  var name: String = ""

  @JsonProperty(JBA_LOGIN)
  var jbaLogin: String = ""

  override fun getFullName(): String {
    return name
  }

  override fun toString(): String {
    return getFullName()
  }

  constructor(userName: String) : this() {
    name = userName
  }
}