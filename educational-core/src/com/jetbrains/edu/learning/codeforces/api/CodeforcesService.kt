package com.jetbrains.edu.learning.codeforces.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface CodeforcesService {
  @GET("api/contest.list")
  fun contests(@Query("gym") trainings: Boolean,
               @Query("locale") locale: String = "en"): Call<ContestsResponse>

  @GET("api/contest.status")
  fun getUserSolutions(@Query("handle") handle: String,
                       @Query("contestId") contestId: Int,
                       @Query("from") from: Int = 1,
                       @Query("count") count: Int = 1000): Call<SubmissionsResponse>

  @GET("contest/{id}")
  fun contest(@Path("id") contestId: Int,
              @Query("locale") locale: String = "en"): Call<ResponseBody>

  @GET("contest/{id}/problems")
  fun problems(@Path("id") contestId: Int,
               @Query("locale") locale: String = "en"): Call<ResponseBody>

  @GET("contest/{id}/status")
  fun status(@Path("id") contestId: Int,
             @Query("locale") locale: String = "en"): Call<ResponseBody>

  @GET("contest/{id}/problem/{position}")
  fun problem(@Path("id") contestId: Int,
              @Path("position") position: Int,
              @Query("locale") locale: String = "en"): Call<ResponseBody>

  @GET("contests")
  fun contestsPage(@Query("locale") locale: String = "en"): Call<ResponseBody>

  @GET("enter?back=%2F")
  fun getLoginPage(): Call<ResponseBody>

  @POST("enter?back=%2F")
  @FormUrlEncoded
  fun postLoginPage(
    @Field("csrf_token") csrfToken: String,
    @Field("action") action: String = "enter",
    @Field("ftaa") ftaa: String = "n/a",
    @Field("bfaa") bfaa: String = "n/a",
    @Field("handleOrEmail") handle: String,
    @Field("password") password: String,
    @Field("remember") remember: String = "on",
    @Header("Cookie") cookie: String): Call<ResponseBody>

  @GET("/contest/{id}/submit")
  fun getSubmissionPage(@Path("id") contestId: Int,
                        @Query("locale") languageCode: String,
                        @Query("programTypeId") programTypeId: String?,
                        @Query("submittedProblemIndex") submittedProblemIndex: String,
                        @Header("Cookie") cookie: String): Call<ResponseBody>

  @POST("/contest/{id}/submit")
  @FormUrlEncoded
  fun postSolution(@Field("csrf_token") csrfToken: String,
                   @Field("action") action: String = "submitSolutionFormSubmitted",
                   @Field("ftaa") ftaa: String = "n/a",
                   @Field("bfaa") bfaa: String = "n/a",
                   @Field("submittedProblemIndex") submittedProblemIndex: String,
                   @Field("source") source: String,
                   @Field("programTypeId") programTypeId: String?,
                   @Path("id") contestId: Int,
                   @Query("csrf_token") csrf_token: String?,
                   @Header("Cookie") cookie: String): Call<ResponseBody>

  @GET("/profile")
  fun profile(@Header("Cookie") jSessionId: String): Call<ResponseBody>

  @POST("/data/submitSource")
  @FormUrlEncoded
  fun getSubmissionSource(@Field("csrf_token") csrfToken: String,
                          @Field("submissionId") submissionId: Int,
                          @Header("Cookie") cookie: String): Call<SubmissionSource>


  @GET("/contestRegistration/{id}")
  fun getRegistrationPage(@Path("id") contestId: Int,
                          @Header("Cookie") jSessionId: String): Call<ResponseBody>

  @POST("/contestRegistration/{id}")
  @FormUrlEncoded
  fun postRegistration(
    @Field("csrf_token") csrfToken: String,
    @Path("id") contestId: Int,
    @Header("Cookie") cookie: String,
    @Field("action") action: String = "formSubmitted",
    @Field("takePartAs") takePartAs: String = "personal"): Call<ResponseBody>


  @GET("/contests/{id}")
  fun getContestRegistrationData(@Path("id") contestId: Int,
                                 @Header("Cookie") jSessionId: String): Call<ResponseBody>
}