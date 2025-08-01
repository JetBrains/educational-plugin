package com.jetbrains.edu.learning.marketplace.courseStorage.api

import com.jetbrains.edu.learning.courseFormat.EduCourse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CourseStorageRepositoryEndpoints {
  @GET("/api/courses/latest")
  fun getCourseDto(@Query("courseId") courseId: Int): Call<EduCourse>
}