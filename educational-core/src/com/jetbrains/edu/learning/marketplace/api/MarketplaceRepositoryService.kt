package com.jetbrains.edu.learning.marketplace.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface MarketplaceRepositoryService {

  @POST("/api/search/graphql")
  fun search(@Body query: QueryData): Call<PluginData>
}