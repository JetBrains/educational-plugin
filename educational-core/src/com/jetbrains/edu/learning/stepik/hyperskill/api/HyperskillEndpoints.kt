package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.AttemptsList
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.api.SubmissionsList
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface HyperskillEndpoints {
  @GET("api/profiles/current")
  fun getCurrentUserInfo(): Call<ProfilesList>

  @GET("api/stages")
  fun stages(@Query("project") projectId: Int): Call<StagesList>

  @GET("api/topics")
  fun topics(@Query("stage") stageId: Int, @Query("page") page: Int): Call<TopicsList>

  @GET("api/steps")
  fun steps(@Query("ids", encoded = true) ids: String): Call<HyperskillStepsList>

  @GET("api/steps")
  fun steps(@Query("topic") topic: Int): Call<HyperskillStepsList>

  @GET("api/submissions")
  fun submission(@Query("user") user: Int, @Query("step", encoded = true) step: String, @Query("page") page: Int): Call<SubmissionsList>

  @GET("api/submissions/{id}")
  fun submission(@Path("id") submissionId: Int): Call<SubmissionsList>

  @GET("api/projects/{id}")
  fun project(@Path("id") projectId: Int): Call<ProjectsList>

  @GET("api/solutions")
  fun solutions(@Query("step") step: Int): Call<SolutionsList>

  @GET("api/users/{id}")
  fun user(@Path("id") id: Int): Call<UsersList>

  @GET("api/attempts")
  fun attempts(@Query("step") stepId: Int, @Query("user") userId: Int): Call<AttemptsList>

  @GET("api/attempts/{dataset_id}/dataset")
  fun dataset(@Path("dataset_id") datasetId: Int): Call<ResponseBody>

  @POST("api/attempts")
  fun attempt(@Body attempt: Attempt): Call<AttemptsList>

  @POST("api/steps/{id}/complete")
  fun completeStep(@Path("id") id: Int): Call<Any>

  @POST("api/submissions")
  fun submission(@Body submission: StepikBasedSubmission): Call<SubmissionsList>

  @POST("api/ws")
  fun websocket(): Call<WebSocketConfiguration>

  @POST("/api/frontend-events")
  fun sendFrontendEvents(@Body events: List<HyperskillFrontendEvent>): Call<HyperskillFrontendEventList>

  @POST("/api/time-spent-events")
  fun sendTimeSpentEvents(@Body events: List<HyperskillTimeSpentEvent>): Call<HyperskillTimeSpentEventList>

  @POST("api/comments")
  fun postComment(@Body comment: HyperskillComment): Call<ResponseBody>
}
