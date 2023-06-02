package com.jetbrains.edu.learning.checkio.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CheckiOApiEndpoint {
  @GET("api/user-missions/")
  fun getMissionList(@Query("token") accessToken: String): Call<CheckiOMissionList>
}
