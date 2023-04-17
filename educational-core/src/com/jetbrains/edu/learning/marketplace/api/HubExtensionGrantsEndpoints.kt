package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.authUtils.TokenInfo
import retrofit2.Call
import retrofit2.http.*

interface HubExtensionGrantsEndpoints {
  @POST("/api/rest/oauth2/token")
  @FormUrlEncoded
  fun exchangeTokens(@Field("grant_type") grantType: String,
                     @Field("token") token: String,
                     @Field("scope") scope: String): Call<TokenInfo>
}