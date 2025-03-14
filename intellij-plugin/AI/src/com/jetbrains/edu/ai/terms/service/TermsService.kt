package com.jetbrains.edu.ai.terms.service

import com.jetbrains.educational.terms.format.CourseTermsResponse
import com.jetbrains.educational.terms.format.domain.TermsVersion
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface TermsService {
  @GET("/api/terms/{marketplaceId}/{updateVersion}/{language}/latest")
  suspend fun getLatestCourseTermsVersion(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String,
  ): Response<TermsVersion>

  @GET("/api/terms/{marketplaceId}/{updateVersion}/{language}")
  suspend fun getCourseTerms(
    @Path("marketplaceId") marketplaceId: Int,
    @Path("updateVersion") updateVersion: Int,
    @Path("language") language: String
  ): Response<CourseTermsResponse>
}