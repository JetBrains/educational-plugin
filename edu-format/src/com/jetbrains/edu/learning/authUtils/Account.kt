package com.jetbrains.edu.learning.authUtils

import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.findService


abstract class Account<UInfo : UserInfo> {
  protected abstract val servicePrefix: String
  val serviceName get() = "$servicePrefix Integration"

  @field:Transient
  lateinit var userInfo: UInfo

  abstract fun isUpToDate(): Boolean
  fun getUserName(): String = userInfo.getFullName()

  protected fun getSecret(userName: String?, serviceNameForPasswordSafe: String?): String? {
    return findService(AccountService::class.java).getSecret(userName, serviceNameForPasswordSafe)
  }
}
