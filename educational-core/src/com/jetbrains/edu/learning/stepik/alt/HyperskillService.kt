package com.jetbrains.edu.learning.stepik.alt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.TokenInfo
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
  fun getUserInfo(@Path("id") userId: Int): Call<UsersData>

  @GET("api/topics/{id}")
  fun topics(@Path("id") id: String = "", @Query("stage") stage: String = ""): Call<TopicsData>

  @GET("api/lessons")
  fun lessons(@Query("topic") topic: Int): Call<LessonsData>

}

@JsonIgnoreProperties(ignoreUnknown = true)
class UsersData {
  lateinit var meta: Any
  lateinit var users: List<HyperskillUserInfo>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TopicsData {
  lateinit var meta: Any
  lateinit var topics: List<Topic>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Topic {
  var id: Int = -1
  var title: String = ""
  lateinit var children: List<Int>

  @JsonProperty("has_lessons") var hasLessons: Boolean = false
}

@JsonIgnoreProperties(ignoreUnknown = true)
class LessonsData {
  lateinit var meta: Any
  lateinit var lessons: List<HyperskillLesson>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class HyperskillLesson {
  @JsonProperty("stepik_id") var stepikId: Int = -1
}
