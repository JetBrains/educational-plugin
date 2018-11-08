package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.jetbrains.edu.learning.authUtils.TokenInfo
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

  @POST("oauth2/token/")
  fun refreshTokens(
    @Query("grant_type") grantType: String,
    @Query("client_id") clientId: String,
    @Query("refresh_token") refreshToken: String
  ): Call<TokenInfo>

  @GET("api/users/{id}")
  fun getUserInfo(@Path("id") userId: Int): Call<UsersData>

  @GET("api/stages")
  fun stages(@Query("project") projectId: Int): Call<StagesData>

  @GET("api/topics")
  fun topics(@Query("stage") stageId: Int): Call<TopicsData>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UsersData {
  lateinit var meta: Any
  lateinit var users: List<HyperskillUserInfo>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class StagesData {
  lateinit var meta: Any
  lateinit var stages: List<HyperskillStage>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TopicsData {
  lateinit var topics: List<HyperskillTopic>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class HyperskillTopic {
  var id: Int = -1
  var title: String = ""
  lateinit var children: List<String>
}

