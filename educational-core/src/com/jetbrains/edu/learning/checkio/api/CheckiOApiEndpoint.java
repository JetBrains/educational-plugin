package com.jetbrains.edu.learning.checkio.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CheckiOApiEndpoint {
  @GET("api/user-missions/")
  Call<CheckiOMissionList> getMissionList(@Query("token") String accessToken);
}
