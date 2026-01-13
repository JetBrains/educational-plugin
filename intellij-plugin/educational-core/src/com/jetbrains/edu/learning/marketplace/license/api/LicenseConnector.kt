package com.jetbrains.edu.learning.marketplace.license.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result

@Service(Service.Level.APP)
class LicenseConnector {
  /**
   * @return Result with a boolean value indicating whether the license is active or an error message
   */
  suspend fun checkLicense(link: String): Result<Boolean, String> {
    return Ok(true)
  }

  companion object {
    fun getInstance(): LicenseConnector = service()
  }
}