package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.api.wrappers.CheckiOMissionListWrapper;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CheckiOApiInterface {
  @GET("api/user-missions/")
  Call<CheckiOMissionListWrapper> getMissionList(@Query("token") String accessToken);
}
