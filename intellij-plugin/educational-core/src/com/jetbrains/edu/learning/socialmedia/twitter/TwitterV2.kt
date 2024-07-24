package com.jetbrains.edu.learning.socialmedia.twitter

import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TwitterV2 {

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
