package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.MarketplaceUserInfo
import retrofit2.Call
import retrofit2.http.*

interface MarketplaceEndpoints {
  @GET("users/{id}?fields=name,guest,id")
  fun getUserInfo(@Path(value = "id") userId: String): Call<MarketplaceUserInfo>
}

interface MarketplaceExtensionGrantsEndpoints {
  @POST("oauth2/token")
  @FormUrlEncoded
  fun exchangeTokens(@Field("grant_type") grantType: String,
                     @Field("token") token: String): Call<TokenInfo>
}