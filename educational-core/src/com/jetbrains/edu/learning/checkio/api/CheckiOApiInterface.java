package com.jetbrains.edu.learning.checkio.api;

import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

public interface CheckiOApiInterface {
  @GET("api/user-missions/")
  Call<List<CheckiOMission>> getMissionList(@Query("token") String accessToken);
}
