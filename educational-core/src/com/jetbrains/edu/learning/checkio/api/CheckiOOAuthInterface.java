package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.account.CheckiOTokens;
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo;
import com.jetbrains.edu.learning.checkio.call.CheckiOCall;
import retrofit2.http.*;

public interface CheckiOOAuthInterface {
  @FormUrlEncoded
  @POST("oauth/token/")
  CheckiOCall<CheckiOTokens> getTokens(
    @Field("grant_type") String grantType,
    @Field("client_secret") String clientSecret,
    @Field("client_id") String clientId,
    @Field("code") String code,
    @Field("redirect_uri") String redirectUri
  );

  @FormUrlEncoded
  @POST("oauth/token/")
  CheckiOCall<CheckiOTokens> refreshTokens(
    @Field("grant_type") String grantType,
    @Field("client_secret") String clientSecret,
    @Field("client_id") String clientId,
    @Field("refresh_token") String refreshToken
  );

  @GET("oauth/information/")
  CheckiOCall<CheckiOUserInfo> getUserInfo(@Query("access_token") String accessToken);
}
