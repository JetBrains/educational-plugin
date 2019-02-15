@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.stepik.api.*
import retrofit2.Call
import retrofit2.http.*

@Suppress("unused")
interface HyperskillService {

  @POST("oauth2/token")
  fun getTokens(
    @Query("client_id") clientId: String,
    @Query("redirect_uri") redirectUri: String,
    @Query("code") code: String,
    @Query("grant_type") grantType: String
  ): Call<TokenInfo>

  @POST("oauth2/token")
  fun refreshTokens(
    @Query("grant_type") grantType: String,
    @Query("client_id") clientId: String,
    @Query("refresh_token") refreshToken: String
  ): Call<TokenInfo>

  @GET("api/users/{id}")
  fun getUserInfo(@Path("id") userId: Int): Call<UsersList>

  @GET("api/stages")
  fun stages(@Query("project") projectId: Int): Call<StagesList>

  @GET("api/topics")
  fun topics(@Query("stage") stageId: Int): Call<TopicsList>

  @GET("api/steps")
  fun steps(@Query("lesson") lessonId: Int): Call<StepsList>

  @POST("api/attempts")
  fun attempt(@Body attempt: Attempt): Call<AttemptsList>

  @POST("api/submissions")
  fun submission(@Body submission: Submission): Call<Any>

  @GET("api/submissions")
  fun submission(@Query("step") step: Int, @Query("page") page: Int): Call<SubmissionsList>
}
