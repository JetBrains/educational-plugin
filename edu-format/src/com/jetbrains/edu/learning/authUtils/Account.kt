package com.jetbrains.edu.learning.authUtils

import com.jetbrains.edu.learning.findService


abstract class Account<UserInfo : Any> {
  protected abstract val servicePrefix: String
  protected val serviceName get() = "$servicePrefix Integration"

  @field:Transient
  lateinit var userInfo: UserInfo

  abstract fun isUpToDate(): Boolean
  protected abstract fun getUserName(): String

  protected fun getSecret(userName: String?, serviceNameForPasswordSafe: String?): String? {
    return findService(AccountService::class.java).getSecret(userName, serviceNameForPasswordSafe)
  }
}
