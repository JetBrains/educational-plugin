package com.jetbrains.edu.learning.codeforces.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CodeforcesService {
  @GET("api/contest.list")
  fun contests(@Query("gym") trainings: Boolean): Call<ContestsList>

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
}