package com.jetbrains.edu.learning.marketplace.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MarketplaceRepositoryService {

  @POST("/api/search/graphql")
  fun search(@Body query: QueryData): Call<CoursesData>

  @POST("/api/search/graphql")
  fun getUpdateId(@Body query: QueryData): Call<UpdateData>

  @Multipart
  @POST("/edu/plugin/upload")
  fun uploadNewCourse(
    @Part file: MultipartBody.Part,
    @Part("licenseUrl") licenseUrl: RequestBody,
  ): Call<CourseBean>

  @Multipart
  @POST("/plugin/uploadPlugin")
  fun uploadCourseUpdate(
    @Part file: MultipartBody.Part,
    @Part("pluginId") courseId: Int,
  ): Call<ResponseBody>
}