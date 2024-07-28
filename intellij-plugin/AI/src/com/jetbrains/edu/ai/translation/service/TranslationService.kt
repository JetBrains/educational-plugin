package com.jetbrains.edu.ai.translation.service

import com.jetbrains.educational.translation.format.CourseTranslation
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TranslationService {
  @GET("$API_TRANSLATE/{marketplaceId}/{updateVersion}/{language}")
  suspend fun getTranslatedCourse(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String
  ): Response<CourseTranslation>

  @GET("$API_TRANSLATE/{marketplaceId}/{updateVersion}/{language}/task/{taskId}")
  suspend fun getTranslatedTask(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String,
    @Path("taskId") taskId: Int
  ): Response<CourseTranslation>

  @POST("$API_TRANSLATE/{marketplaceId}/{updateVersion}/{language}")
  suspend fun translateCourse(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String,
    @Query("force") force: Boolean = false
  ): Response<Unit>

  companion object {
    private const val API_TRANSLATE = "/api/translate"
  }
}