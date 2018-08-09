package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.api.wrappers.CheckiOUserInfoWrapper;
import com.jetbrains.edu.learning.checkio.api.wrappers.TokensWrapper;
import retrofit2.Call;
import retrofit2.http.*;

public interface CheckiOOAuthInterface {
  @FormUrlEncoded
  @POST("oauth/token/")
  Call<TokensWrapper> getTokens(
    @Field("grant_type") String grantType,
    @Field("client_secret") String clientSecret,
    @Field("client_id") String clientId,
    @Field("code") String code,
    @Field("redirect_uri") String redirectUri
  );

  @FormUrlEncoded
  @POST("oauth/token/")
  Call<TokensWrapper> refreshTokens(
    @Field("grant_type") String grantType,
    @Field("client_secret") String clientSecret,
    @Field("client_id") String clientId,
    @Field("refresh_token") String refreshToken
  );

  @GET("oauth/information/")
  Call<CheckiOUserInfoWrapper> getUserInfo(@Query("access_token") String accessToken);
}
