@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.stepik.StepikSteps
import retrofit2.Call
import retrofit2.http.*

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

  @GET("api/users/{id}/")
  fun getUserInfo(@Path("id") userId: Int): Call<UsersList>

  @GET("api/stages/")
  fun stages(@Query("project") projectId: Int): Call<StagesList>

  @GET("api/topics/")
  fun topics(@Query("stage") stageId: Int): Call<TopicsList>

  @GET("api/steps/")
  fun steps(@Query("lesson") lessonId: Int): Call<StepikSteps.StepsList>

  @POST("api/attempts/")
  fun attempt(@Query("step") stepId: Int): Call<AttemptsList>

  @POST("api/submissions/")
  fun submission(@Body submission: Submission): Call<Any>

}

class UsersList {
  lateinit var meta: Any
  lateinit var users: List<HyperskillUserInfo>
}

class StagesList {
  lateinit var meta: Any
  lateinit var stages: List<HyperskillStage>
}

class TopicsList {
  lateinit var topics: List<HyperskillTopic>
}

class HyperskillTopic {
  var id: Int = -1
  var title: String = ""
  lateinit var children: List<String>
}

class AttemptsList {
  lateinit var meta: Any
  lateinit var attempts: List<Attempt>
}

class Attempt {
  var step: Int = 0
  var id: Int = 0
}

class SolutionFile(var name: String, var text: String)

class Submission(var attempt: Int, var reply: Reply)

class Reply(var score: String, var solution: ArrayList<SolutionFile>)