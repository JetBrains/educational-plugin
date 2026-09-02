package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.network.NetworkResult
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LearningCenterEndpoints {

  /**
   * Requires an authorized user.
   */
  @POST("/api/courses/progress/certificates/issue")
  suspend fun tryIssueCertificate(
    @Query("completedPercent") completedPercent: Int,
    @Query("courseId") courseId: Int
  ): NetworkResult<CourseCertificateIssueResponse>

  /**
   * Works without authorization.
   */
  @GET("/api/courses/progress/certificates/has-certificate")
  suspend fun courseHasCertificate(@Query("courseId") courseId: Int): NetworkResult<CourseCertificationSupportResponse>
}
