package com.jetbrains.edu.ai.translation.service

import com.jetbrains.educational.translation.format.CourseTranslation
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface TranslationService {
  @GET("$API_TRANSLATE/{marketplaceId}/{updateVersion}/{language}")
  suspend fun getTranslatedCourse(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String
  ): Response<CourseTranslation>

  companion object {
    private const val API_TRANSLATE = "/api/translate"
  }
}