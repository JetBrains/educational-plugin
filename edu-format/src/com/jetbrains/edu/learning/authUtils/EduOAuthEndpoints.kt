package com.jetbrains.edu.learning.authUtils

import retrofit2.Call
import retrofit2.http.*

interface EduOAuthEndpoints {
  @POST
  @FormUrlEncoded
  fun getTokens(
    @Url url: String,
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("redirect_uri") redirectUri: String,
    @Field("code") code: String,
    @Field("grant_type") grantType: String,
    @FieldMap codeVerifier: Map<String, String>
  ): Call<TokenInfo>

  @POST
  @FormUrlEncoded
  fun refreshTokens(
    @Url url: String,
    @Field("grant_type") grantType: String,
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("refresh_token") refreshToken: String
  ): Call<TokenInfo>
}

