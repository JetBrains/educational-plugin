@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import retrofit2.Call
import retrofit2.http.*

interface StepikService {
  @GET("stepics/1/")
  fun getCurrentUser(): Call<UsersList>

  @POST("enrollments")
  fun enrollments(@Body enrollment: EnrollmentData): Call<Any>

  @GET("enrollments/{id}/")
  fun enrollments(@Path("id") courseId: Int): Call<Any>

  @GET("courses")
  fun courses(@Query("is_idea_compatible") isIdeaCompatible: Boolean,
              @Query("is_public") isPublic: Boolean,
              @Query("page") page: Int,
              @Query("enrolled") enrolled: Boolean?): Call<CoursesList>
}

class UsersList {
  lateinit var meta: Any
  lateinit var users: List<StepikUserInfo>
}

class Enrollment(var course: String)

class EnrollmentData(courseId: Int) {
  var enrollment: Enrollment = Enrollment(courseId.toString())
}

class CoursesList {
  lateinit var meta: Map<Any, Any>
  lateinit var courses: MutableList<EduCourse>
}