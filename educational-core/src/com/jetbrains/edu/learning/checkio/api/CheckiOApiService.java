package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.model.CheckiOMissionList;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import retrofit2.Call;
import retrofit2.http.*;

public interface CheckiOApiService {

  @FormUrlEncoded
  @POST("oauth/token/")
  Call<Tokens> getTokens(
    @Field("grant_type") String grantType,
    @Field("client_secret") String clientSecret,
    @Field("client_id") String clientId,
    @Field("code") String code,
    @Field("redirect_uri") String redirectUri
  );

  @FormUrlEncoded
  @POST("oauth/token/")
  Call<Tokens> refreshTokens(
    @Field("grant_type") String grantType,
    @Field("client_secret") String clientSecret,
    @Field("client_id") String clientId,
    @Field("refresh_token") String refreshToken
  );

  @GET("oauth/information/")
  Call<CheckiOUser> getUserInfo(@Query("access_token") String accessToken);

  @GET("api/user-missions/")
  Call<CheckiOMissionList> getMissionList(@Query("token") String accessToken);
}
