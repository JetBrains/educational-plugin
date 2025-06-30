package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.BaseState

class LTISettings : BaseState() {
  var launchId: String? by string()
  var lmsDescription: String? by string()
  var onlineService: LTIOnlineService by enum(LTIOnlineService.ALPHA_TEST_2024)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as LTISettings

    if (launchId != other.launchId) return false
    if (lmsDescription != other.lmsDescription) return false
    if (onlineService != other.onlineService) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (launchId?.hashCode() ?: 0)
    result = 31 * result + (lmsDescription?.hashCode() ?: 0)
    result = 31 * result + onlineService.hashCode()
    return result
  }
}
