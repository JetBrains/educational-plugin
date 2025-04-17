package com.jetbrains.edu.learning.socialMedia.x.api

import com.fasterxml.jackson.annotation.JsonProperty

data class Tweet(
  @get:JsonProperty("text") val text: String,
  @get:JsonProperty("media") val media: Media
)

data class Media(
  @get:JsonProperty("media_ids") val mediaIds: List<String>
)

data class TweetResponse(
  @JsonProperty("data") val data: TweetData
)

data class TweetData(
  @JsonProperty("id") val id: String,
  @JsonProperty("text") val text: String
)
