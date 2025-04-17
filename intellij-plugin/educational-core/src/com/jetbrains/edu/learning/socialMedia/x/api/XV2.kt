package com.jetbrains.edu.learning.socialMedia.x.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface XV2 {
  /**
   * Returns information about the requesting User
   *
   * [User lookup me](https://docs.x.com/x-api/users/user-lookup-me)
   */
  @GET("/2/users/me")
  fun usersMe(): Call<XUserLookup>

  /**
   * Causes the User to create a Post under the authorized account
   *
   * [Creation of a Post](https://docs.x.com/x-api/posts/creation-of-a-post)
   */
  @POST("/2/tweets")
  fun postTweet(@Body tweet: Tweet): Call<TweetResponse>
}
