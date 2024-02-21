package com.jetbrains.edu.learning.marketplace.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface SubmissionsService {

  @GET("/api/course/{marketplaceId}/submission")
  fun getAllSubmissionsForCourse(@Path("marketplaceId") marketplaceId: Int,
                                 @Query("page") page: Int): Call<MarketplaceSubmissionsList>

  @GET("/api/course/{marketplaceId}/{updateVersion}/submissions/public")
  fun getAllPublicSubmissionsForCourse(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Query("page") page: Int
  ): Call<MarketplaceSubmissionsList>

  @GET("/api/course/{marketplaceId}/{updateVersion}/task/{taskId}/submissions/public")
  fun getPublicSubmissionsForTask(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("taskId") taskId: Int,
    @Query("page") page: Int
  ): Call<MarketplaceSubmissionsList>

  @POST("/api/course/{marketplaceId}/{courseUpdateVersion}/task/{taskId}/submission")
  fun postSubmission(@Path("marketplaceId") marketplaceId: Int,
                     @Path("courseUpdateVersion") courseUpdateVersion: Int,
                     @Path("taskId") taskId: Int,
                     @Body submission: MarketplaceSubmission): Call<MarketplaceSubmission>

  @GET("/api/solution")
  fun getSolutionDownloadLink(@Query("solutionKey") solutionKey: String): Call<ResponseBody>

  @DELETE("/api/course/submission")
  fun deleteAllSubmissions(): Call<ResponseBody>

  @GET("/api/user/sharing")
  suspend fun getSharingPreference(): ResponseBody

  @PATCH("/api/user/sharing")
  suspend fun changeSharingPreference(@Query("preference") sharingPreference: String): Response<Unit>

  @PATCH("/api/submission/{submissionId}/report")
  fun reportSolution(@Path("submissionId") submissionId: Int): Call<ResponseBody>

  @GET("/api/user/agreement")
  fun getUserAgreementState(): Call<ResponseBody>

  @PATCH("/api/user/agreement")
  suspend fun changeUserAgreementState(@Query("state") agreementState: String): Response<Unit>

  @GET("/api/user/statisticsAllowed")
  suspend fun getUserStatisticsAllowedState(): Response<Boolean>

  @PATCH("/api/user/statisticsAllowed")
  suspend fun changeUserStatisticsAllowedState(@Query("state") statisticsAllowed: Boolean): Response<Unit>
}