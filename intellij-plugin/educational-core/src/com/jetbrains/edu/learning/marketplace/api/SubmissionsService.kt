package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.submissions.UserAgreementState
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

  @GET("/api/course/{marketplaceId}/{updateVersion}/task/{eduId}/submissions/public")
  fun getSharedSubmissionsForTask(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("eduId") eduId: Int
  ): Call<MarketplaceSubmissionsList>

  @GET("/api/course/{marketplaceId}/{updateVersion}/task/{taskId}/submissions/shared")
  fun getMoreSharedSubmissionsForTask(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("taskId") taskId: Int,
    @Query("latest") latest: Int,
    @Query("oldest") oldest: Int
  ): Call<MarketplaceSubmissionsList>

  @POST("/api/course/{marketplaceId}/{courseUpdateVersion}/task/{taskId}/submission")
  fun postSubmission(@Path("marketplaceId") marketplaceId: Int,
                     @Path("courseUpdateVersion") courseUpdateVersion: Int,
                     @Path("taskId") taskId: Int,
                     @Body submission: MarketplaceSubmission): Call<MarketplaceSubmission>

  @GET("/api/course/{marketplaceId}/state")
  fun getStateOnClose(@Path("marketplaceId") marketplaceId: Int,
                      @Query("page") page: Int): Call<MarketplaceStateOnCloseList>

  @POST("/api/course/{marketplaceId}/{courseUpdateVersion}/state")
  fun postStateOnClose(@Path("marketplaceId") marketplaceId: Int,
                       @Path("courseUpdateVersion") courseUpdateVersion: Int,
                       @Body state: List<MarketplaceStateOnClosePost>): Call<Any>

  @GET("/api/solution")
  fun getSolutionDownloadLink(@Query("solutionKey") solutionKey: String): Call<ResponseBody>

  @DELETE("/api/course/submission")
  fun deleteAllSubmissions(): Call<ResponseBody>

  @DELETE("/api/course/{courseId}/submission")
  fun deleteAllSubmissions(@Path("courseId") courseId: Int): Call<ResponseBody>

  @PATCH("/api/user/sharing")
  suspend fun changeSharingPreference(@Query("preference") sharingPreference: String): Response<Unit>

  @PATCH("/api/submission/{submissionId}/report")
  fun reportSolution(@Path("submissionId") submissionId: Int): Call<ResponseBody>

  @PATCH("/api/user/agreement")
  suspend fun changeUserAgreementState(@Query("state") agreementState: String): Response<Unit>

  @PATCH("/api/v2/agreement/update")
  suspend fun updateUserAgreement(
    @Query("pluginAgreement") pluginAgreement: String,
    @Query("aiAgreement") aiAgreement: String
  ): Response<Unit>

  @GET("/api/v2/agreement")
  suspend fun getUserAgreement(): Response<UserAgreement>

  @GET("/api/user/issue-jwt")
  suspend fun issueJWT(): Response<String>
}

/**
 * A separate interface with the API that doesn't require an authorization.
 */
interface RemoteStatisticsService {
  @POST("/api/v2/agreement/save-anonymously")
  suspend fun saveAgreementAcceptanceAnonymously(@Query(value = "isLoggedIn") isLoggedIn: Boolean): Response<Unit>
}

data class UserAgreement(
  @JsonProperty("pluginAgreement") val pluginAgreement: UserAgreementState,
  @JsonProperty("aiTermsOfService") val aiTermsOfService: UserAgreementState
)
