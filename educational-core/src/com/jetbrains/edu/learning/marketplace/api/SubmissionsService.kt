package com.jetbrains.edu.learning.marketplace.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface SubmissionsService {

  @GET("/api/course/{marketplaceId}/submission")
  fun getAllSubmissionsForCourse(@Path("marketplaceId") marketplaceId: Int,
                                 @Query("page") page: Int): Call<MarketplaceSubmissionsList>

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
  fun getSharingPreference(): Call<ResponseBody>

  @PATCH("/api/user/sharing")
  fun changeSharingPreference(@Query("preference") sharingPreference: String): Call<ResponseBody>
}