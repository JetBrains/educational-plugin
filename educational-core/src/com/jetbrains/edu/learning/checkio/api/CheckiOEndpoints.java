package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CheckiOEndpoints {
  @GET("oauth/information/")
  Call<CheckiOUserInfo> getUserInfo(@Query("access_token") String accessToken);
}
