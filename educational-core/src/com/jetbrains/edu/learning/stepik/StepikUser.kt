package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.TokenInfo

class StepikUser : OAuthAccount<StepikUserInfo> {
  private constructor()
  constructor(tokenInfo: TokenInfo) : super(tokenInfo.expiresIn)

  @Suppress("UnstableApiUsage")
  override val servicePrefix: @NlsSafe String = StepikNames.STEPIK

  @get:Transient
  val id: Int
    get() {
      return userInfo.id
    }

  @get:Transient
  val firstName: String
    get() {
      return userInfo.firstName
    }

  @get:Transient
  val lastName: String
    get() {
      return userInfo.lastName
    }

  @get:Transient
  val name: String
    get() {
      return arrayOf(userInfo.firstName, userInfo.lastName).joinToString(" ")
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val user = other as StepikUser
    val otherInfo = user.userInfo
    return userInfo == otherInfo
  }

  override fun hashCode(): Int = userInfo.hashCode()

  companion object {
    fun createEmptyUser(): StepikUser {
      return StepikUser()
    }
  }
}
