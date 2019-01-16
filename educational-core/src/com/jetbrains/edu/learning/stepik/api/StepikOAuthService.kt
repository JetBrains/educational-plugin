@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.authUtils.TokenInfo
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface StepikOAuthService {

  @POST("oauth2/token/")
  fun getTokens(
    @Query("client_id") clientId: String,
    @Query("redirect_uri") redirectUri: String,
    @Query("code") code: String,
    @Query("grant_type") grantType: String
  ): Call<TokenInfo>

  @POST("oauth2/token/")
  fun refreshTokens(
    @Query("grant_type") grantType: String,
    @Query("client_id") clientId: String,
    @Query("refresh_token") refreshToken: String
  ): Call<TokenInfo>
}

