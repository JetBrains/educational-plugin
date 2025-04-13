package com.jetbrains.edu.learning.socialMedia.x.api

import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.ResponseBody
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

  @POST("/2/tweets")
  fun postTweet(@Body tweet: Tweet): Call<ResponseBody>
}

data class Tweet(
  val text: String,
  val media: Media
)

data class Media(
  @get:JsonProperty("media_ids")
  val mediaIds: List<String>
)
