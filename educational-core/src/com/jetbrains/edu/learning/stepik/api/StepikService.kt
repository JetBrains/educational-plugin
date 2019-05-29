@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface StepikService {

  // GET requests:

  @GET("api/stepics/1/")
  fun getCurrentUser(): Call<UsersList>

  @GET("api/enrollments/{id}/")
  fun enrollments(@Path("id") courseId: Int): Call<Any>

  @GET("api/courses")
  fun courses(@Query("is_idea_compatible") isIdeaCompatible: Boolean,
              @Query("is_public") isPublic: Boolean,
              @Query("page") page: Int,
              @Query("enrolled") enrolled: Boolean?): Call<CoursesList>

  @GET("api/courses/{id}")
  fun courses(@Path("id") courseId: Int,
              @Query("is_idea_compatible") isIdeaCompatible: Boolean?): Call<CoursesList>

  @GET("api/users")
  fun users(@Query("ids[]") vararg ids: Int): Call<UsersList>

  @GET("api/sections")
  fun sections(@Query("ids[]") vararg ids: Int): Call<SectionsList>

  @GET("api/lessons")
  fun lessons(@Query("ids[]") vararg ids: Int): Call<LessonsList>

  @GET("api/units")
  fun units(@Query("ids[]") vararg ids: Int): Call<UnitsList>

  @GET("api/units")
  fun lessonUnit(@Query("lesson") lesson: Int): Call<UnitsList>

  @GET("api/steps")
  fun steps(@Query("ids[]") vararg ids: Int): Call<StepsList>

  @GET("api/progresses")
  fun progresses(@Query("ids[]") vararg ids: String): Call<ProgressesList>

  @GET("api/submissions")
  fun submissions(@Query("order") order: String = "desc",
                  @Query("page") page: Int = 1,
                  @Query("status") status: String,
                  @Query("step") step: Int): Call<SubmissionsList>

  @GET("api/submissions")
  fun submissions(@Query("order") order: String = "desc",
                  @Query("attempt") attempt: Int = 1,
                  @Query("user") user: Int): Call<SubmissionsList>

  @GET("api/attempts")
  fun attempts(@Query("step") stepId: Int, @Query("user") userId: Int): Call<AttemptsList>

  @GET("api/assignments")
  fun assignments(@Query("ids[]") vararg ids: Int): Call<AssignmentsList>

  @GET("api/attachments")
  fun attachments(@Query("course") courseId: Int): Call<AttachmentsList>

  @GET("api/step-sources/{id}")
  fun choiceStepSource(@Path("id") stepId: Int): Call<ChoiceStepSourcesList>

  // POST requests:

  @POST("api/courses")
  fun course(@Body sectionData: CourseData): Call<CoursesList>

  @POST("api/sections")
  fun section(@Body sectionData: SectionData): Call<SectionsList>

  @POST("api/lessons")
  fun lesson(@Body lessonData: LessonData): Call<LessonsList>

  @POST("api/units")
  fun unit(@Body unitData: UnitData): Call<UnitsList>

  @POST("api/step-sources")
  fun stepSource(@Body stepSourceData: StepSourceData): Call<StepSourcesList>

  @POST("api/enrollments")
  fun enrollment(@Body enrollment: EnrollmentData): Call<Any>

  @POST("api/submissions")
  fun submission(@Body submissionData: SubmissionData): Call<SubmissionsList>

  @POST("api/attempts")
  fun attempt(@Body attemptData: AttemptData): Call<AttemptsList>

  @POST("api/views")
  fun view(@Body viewData: ViewData): Call<ResponseBody>

  @POST("api/members")
  fun members(@Body membersData: MemberData): Call<ResponseBody>

  @Multipart
  @POST("api/attachments")
  fun attachment(@Part file: MultipartBody.Part, @Part("course") course: RequestBody): Call<ResponseBody>

  // PUT requests:

  @PUT("api/courses/{id}")
  fun course(@Path("id") courseId: Int, @Body courseData: CourseData): Call<CoursesList>

  @PUT("api/sections/{id}")
  fun section(@Path("id") sectionId: Int, @Body sectionData: SectionData): Call<SectionsList>

  @PUT("api/lessons/{id}")
  fun lesson(@Path("id") lessonId: Int, @Body lessonData: LessonData): Call<LessonsList>

  @PUT("api/units/{id}")
  fun unit(@Path("id") unitId: Int, @Body unitData: UnitData): Call<UnitsList>

  @PUT("api/step-sources/{id}")
  fun stepSource(@Path("id") stepSourceId: Int, @Body stepSourceData: StepSourceData): Call<StepSourcesList>

  // DELETE requests:

  @DELETE("api/sections/{id}")
  fun deleteSection(@Path("id") sectionId: Int): Call<Any>

  @DELETE("api/lessons/{id}")
  fun deleteLesson(@Path("id") lessonId: Int): Call<Any>

  @DELETE("api/units/{id}")
  fun deleteUnit(@Path("id") unitId: Int): Call<Any>

  @DELETE("api/step-sources/{id}")
  fun deleteStepSource(@Path("id") taskId: Int): Call<Any>

  @DELETE("api/attachments/{id}")
  fun deleteAttachment(@Path("id") attachmentId: Int): Call<Any>

}
