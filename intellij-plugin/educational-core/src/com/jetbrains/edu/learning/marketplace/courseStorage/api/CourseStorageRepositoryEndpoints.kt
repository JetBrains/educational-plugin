package com.jetbrains.edu.learning.marketplace.courseStorage.api

import com.jetbrains.edu.learning.courseFormat.EduCourse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface CourseStorageRepositoryEndpoints {
  @GET("/api/courses/latest")
  fun getCourseDto(@Query("courseId") courseId: Int): Call<EduCourse>

  @Multipart
  @POST("/api/courses/upload")
  fun uploadCourse(@Part courseArchive: MultipartBody.Part): Call<EduCourse>
}