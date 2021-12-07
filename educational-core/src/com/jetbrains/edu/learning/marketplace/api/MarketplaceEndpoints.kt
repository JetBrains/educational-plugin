package com.jetbrains.edu.learning.marketplace.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface MarketplaceEndpoints {
  @GET("users/{id}?fields=name,guest,id")
  fun getCurrentUserInfo(@Path(value = "id") userId: String): Call<MarketplaceUserInfo>
}