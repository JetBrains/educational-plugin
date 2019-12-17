@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.AttemptsList
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.api.SubmissionsList
import retrofit2.Call
import retrofit2.http.*

@Suppress("unused")
interface HyperskillService {

  @POST("oauth2/token/")
  @FormUrlEncoded
  fun getTokens(@Field("client_id") clientId: String,
                @Field("client_secret") clientSecret: String,
                @Field("redirect_uri") redirectUri: String,
                @Field("code") code: String,
                @Field("grant_type") grantType: String): Call<TokenInfo>

  @POST("oauth2/token/")
  @FormUrlEncoded
  fun refreshTokens(
    @Field("grant_type") grantType: String,
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("refresh_token") refreshToken: String
  ): Call<TokenInfo>

  @GET("api/profiles/current")
  fun getCurrentUserInfo(): Call<UsersList>

  @GET("api/stages")
  fun stages(@Query("project") projectId: Int): Call<StagesList>

  @GET("api/topics")
  fun topics(@Query("stage") stageId: Int, @Query("page") page: Int): Call<TopicsList>

  @GET("api/steps/{id}")
  fun step(@Path("id") stepId: Int): Call<HyperskillStepsList>

  @GET("api/submissions")
  fun submission(@Query("step") step: Int, @Query("page") page: Int): Call<SubmissionsList>

  @GET("api/submissions/{id}")
  fun submission(@Path("id") submissionId: Int): Call<SubmissionsList>

  @GET("api/projects/{id}")
  fun project(@Path("id") projectId: Int): Call<ProjectsList>

  @GET("api/solutions")
  fun solutions(@Query("step") step: Int): Call<SolutionsList>

  @POST("api/attempts")
  fun attempt(@Body attempt: Attempt): Call<AttemptsList>

  @POST("api/submissions")
  fun submission(@Body submission: Submission): Call<SubmissionsList>
}
