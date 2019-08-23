@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.authUtils.TokenInfo
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface StepikOAuthService {

  @POST("oauth2/token/")
  @FormUrlEncoded
  fun getTokens(
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("redirect_uri") redirectUri: String,
    @Field("code") code: String,
    @Field("grant_type") grantType: String
  ): Call<TokenInfo>

  @POST("oauth2/token/")
  @FormUrlEncoded
  fun refreshTokens(
    @Field("grant_type") grantType: String,
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("refresh_token") refreshToken: String
  ): Call<TokenInfo>
}

