package com.jetbrains.edu.learning.marketplace.license.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface LicenseEndpoints {
  @GET
  suspend fun checkLicense(@Url url: String): Response<LicenseCheckResponse>
}