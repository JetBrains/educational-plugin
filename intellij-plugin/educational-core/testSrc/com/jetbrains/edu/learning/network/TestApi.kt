package com.jetbrains.edu.learning.network

import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Call
import retrofit2.http.GET

interface TestApi {
  @GET("/call")
  fun call(): Call<Answer>
}

data class Answer(@param:JsonProperty("value") val value: String)
