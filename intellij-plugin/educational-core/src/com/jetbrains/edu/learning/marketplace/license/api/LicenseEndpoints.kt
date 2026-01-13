package com.jetbrains.edu.learning.marketplace.license.api

import retrofit2.Response
import retrofit2.http.GET

interface LicenseEndpoints {
  @GET
  suspend fun checkLicense(): Response<LicenseCheckResponse>
}