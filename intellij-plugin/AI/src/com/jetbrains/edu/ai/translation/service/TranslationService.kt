package com.jetbrains.edu.ai.translation.service

import com.jetbrains.educational.translation.format.CourseTranslationResponse
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface TranslationService {
  @GET("$API_TRANSLATE/{marketplaceId}/{updateVersion}/{language}/latest")
  suspend fun getLatestCourseTranslationVersion(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String,
  ): Response<TranslationVersion>

  @GET("$API_TRANSLATE/{marketplaceId}/{updateVersion}/{language}")
  suspend fun getTranslatedCourse(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String
  ): Response<CourseTranslationResponse>

  companion object {
    private const val API_TRANSLATE = "/api/translate"
  }
}