package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.model.CheckioUser;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface CheckioApiService {

  @Headers({"Content-Type: application/x-www-form-urlencoded"})
  @POST("oauth/token/")
  Call<Tokens> getTokens(
    @Query("grant_type") String grantType,
    @Query("client_secret") String clientSecret,
    @Query("client_id") String clientId,
    @Query("code") String code,
    @Query("redirect_uri") String redirectUri
  );

  @Headers({"Content-Type: application/x-www-form-urlencoded"})
  @POST("oauth/token/")
  Call<Tokens> refreshTokens(
    @Query("grant_type") String grantType,
    @Query("client_secret") String clientSecret,
    @Query("client_id") String clientId,
    @Query("refresh_token") String refreshToken
  );

  @GET("oauth/information/")
  Call<CheckioUser> getUserInfo(@Query("access_token") String accessToken);
}
