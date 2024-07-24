package com.jetbrains.edu.learning.socialmedia.linkedIn

import retrofit2.Call
import retrofit2.http.GET

interface LinkedInEndpoints {
  @GET("/v2/userinfo")
  fun getCurrentUserInfo(): Call<LinkedInUserInfo>
}