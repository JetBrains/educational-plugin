package com.jetbrains.edu.learning.stepik.alt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Suppress("unused")
interface HyperskillService {

  @POST("oauth2/token/")
  fun getTokens(
    @Query("client_id") clientId: String,
    @Query("redirect_uri") redirectUri: String,
    @Query("code") code: String,
    @Query("grant_type") grantType: String
  ): Call<TokenInfo>

  @GET("api/users/{id}")
  fun getUserInfo(@Path("id") userId: Int): Call<Data>

  @GET("api/recommendations/")
  fun recommendations(): Call<Recommendation>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Data {
  lateinit var meta: Any
  lateinit var users: List<HyperskillUserInfo>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TokenInfo {
  @JsonProperty("access_token")
  var accessToken: String = ""
  @JsonProperty("refresh_token")
  var refreshToken: String = ""
}


@JsonIgnoreProperties(ignoreUnknown = true)
class Recommendation {
  var lesson: Lesson? = null
}

class Lesson {
  @JsonProperty("stepik_id")
  var stepikId: Int = 0
}
