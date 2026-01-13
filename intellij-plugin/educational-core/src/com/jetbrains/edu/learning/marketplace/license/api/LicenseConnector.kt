package com.jetbrains.edu.learning.marketplace.license.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.marketplace.license.LicenseState

@Service(Service.Level.APP)
class LicenseConnector {
  /**
   * @return Result with a boolean value indicating whether the license is active or an error message
   */
  suspend fun checkLicense(link: String): LicenseState {
    return TODO("Implement license check logic in next review")
  }

  companion object {
    fun getInstance(): LicenseConnector = service()
  }
}