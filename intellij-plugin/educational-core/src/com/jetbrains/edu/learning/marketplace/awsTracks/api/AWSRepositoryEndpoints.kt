package com.jetbrains.edu.learning.marketplace.awsTracks.api

import com.jetbrains.edu.learning.courseFormat.EduCourse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AWSRepositoryEndpoints {
  @GET("/api/courses/latest")
  fun getCourseDto(@Query("courseId") courseId: Int): Call<EduCourse>
}