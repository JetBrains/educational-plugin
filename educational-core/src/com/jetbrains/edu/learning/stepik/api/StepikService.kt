@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.stepik.StepikUserInfo
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StepikService {
  @GET("stepics/1/")
  fun getCurrentUser(): Call<UsersList>

  @POST("enrollments")
  fun enrollments(@Body enrollment: EnrollmentData): Call<Any>

  @GET("enrollments/{id}/")
  fun enrollments(@Path("id") courseId: Int): Call<Any>
}

class UsersList {
  lateinit var meta: Any
  lateinit var users: List<StepikUserInfo>
}

class Enrollment(var course: String)

class EnrollmentData(courseId: Int) {
  var enrollment: Enrollment = Enrollment(courseId.toString())
}