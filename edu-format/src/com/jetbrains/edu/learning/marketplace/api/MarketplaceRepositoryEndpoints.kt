package com.jetbrains.edu.learning.marketplace.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MarketplaceRepositoryEndpoints {

  @POST("/api/search/graphql")
  fun search(@Body query: QueryData): Call<CoursesData>

  @POST("/api/search/graphql")
  fun getUpdateId(@Body query: QueryData): Call<UpdateData>

  @Multipart
  @POST("/api/plugins/edu/upload/details")
  fun uploadNewCourse(
    @Part file: MultipartBody.Part,
    @Part("licenseUrl") licenseUrl: RequestBody
  ): Call<UploadResponse>

  @Multipart
  @POST("/api/updates/upload/details")
  fun uploadCourseUpdate(
    @Part file: MultipartBody.Part,
    @Part("pluginId") courseId: Int,
  ): Call<UploadResponse>
}