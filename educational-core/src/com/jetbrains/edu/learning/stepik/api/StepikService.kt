@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface StepikService {

  // GET requests:

  @GET("stepics/1/")
  fun getCurrentUser(): Call<UsersList>

  @GET("enrollments/{id}/")
  fun enrollments(@Path("id") courseId: Int): Call<Any>

  @GET("courses")
  fun courses(@Query("is_idea_compatible") isIdeaCompatible: Boolean,
              @Query("is_public") isPublic: Boolean,
              @Query("page") page: Int,
              @Query("enrolled") enrolled: Boolean?): Call<CoursesList>

  @GET("courses/{id}")
  fun courses(@Path("id") courseId: Int,
              @Query("is_idea_compatible") isIdeaCompatible: Boolean?): Call<CoursesList>

  @GET("users")
  fun users(@Query("ids[]") vararg ids: Int): Call<UsersList>

  @GET("sections")
  fun sections(@Query("ids[]") vararg ids: Int): Call<SectionsList>

  @GET("lessons")
  fun lessons(@Query("ids[]") vararg ids: Int): Call<LessonsList>

  @GET("units")
  fun units(@Query("ids[]") vararg ids: Int): Call<UnitsList>

  @GET("units")
  fun lessonUnit(@Query("lesson") lesson: Int): Call<UnitsList>

  @GET("steps")
  fun steps(@Query("ids[]") vararg ids: Int): Call<StepsList>

  @GET("progresses")
  fun progresses(@Query("ids[]") vararg ids: String): Call<ProgressesList>

  @GET("submissions")
  fun submissions(@Query("order") order: String = "desc",
                  @Query("page") page: Int = 1,
                  @Query("status") status: String,
                  @Query("step") step: Int): Call<SubmissionsList>

  @GET("submissions")
  fun submissions(@Query("order") order: String = "desc",
                  @Query("attempt") attempt: Int = 1,
                  @Query("user") user: Int): Call<SubmissionsList>

  @GET("attempts")
  fun attempts(@Query("step") stepId: Int, @Query("user") userId: Int): Call<AttemptsList>

  @GET("assignments")
  fun assignments(@Query("ids[]") vararg ids: Int): Call<AssignmentsList>

  @GET("attachments")
  fun attachments(@Query("course") courseId: Int): Call<AttachmentsList>


  // POST requests:

  @POST("courses")
  fun course(@Body sectionData: CourseData): Call<CoursesList>

  @POST("sections")
  fun section(@Body sectionData: SectionData): Call<SectionsList>

  @POST("lessons")
  fun lesson(@Body lessonData: LessonData): Call<LessonsList>

  @POST("units")
  fun unit(@Body unitData: UnitData): Call<UnitsList>

  @POST("step-sources")
  fun stepSource(@Body stepSourceData: StepSourceData): Call<StepSourcesList>

  @POST("enrollments")
  fun enrollment(@Body enrollment: EnrollmentData): Call<Any>

  @POST("submissions")
  fun submission(@Body submissionData: SubmissionData): Call<SubmissionsList>

  @POST("attempts")
  fun attempt(@Body attemptData: AttemptData): Call<AttemptsList>

  @POST("views")
  fun view(@Body viewData: ViewData): Call<ResponseBody>

  @POST("members")
  fun members(@Body membersData: MemberData): Call<ResponseBody>

  @Multipart
  @POST("attachments")
  fun attachment(@Part file: MultipartBody.Part, @Part("course") course: RequestBody): Call<ResponseBody>

  // PUT requests:

  @PUT("courses/{id}")
  fun course(@Path("id") courseId: Int, @Body courseData: CourseData): Call<CoursesList>

  @PUT("sections/{id}")
  fun section(@Path("id") sectionId: Int, @Body sectionData: SectionData): Call<SectionsList>

  @PUT("lessons/{id}")
  fun lesson(@Path("id") lessonId: Int, @Body lessonData: LessonData): Call<LessonsList>

  @PUT("units/{id}")
  fun unit(@Path("id") unitId: Int, @Body unitData: UnitData): Call<UnitsList>

  @PUT("step-sources/{id}")
  fun stepSource(@Path("id") stepSourceId: Int, @Body stepSourceData: StepSourceData): Call<Any>

  // DELETE requests:

  @DELETE("sections/{id}")
  fun deleteSection(@Path("id") sectionId: Int): Call<Any>

  @DELETE("lessons/{id}")
  fun deleteLesson(@Path("id") lessonId: Int): Call<Any>

  @DELETE("units/{id}")
  fun deleteUnit(@Path("id") unitId: Int): Call<Any>

  @DELETE("step-sources/{id}")
  fun deleteStepSource(@Path("id") taskId: Int): Call<Any>

  @DELETE("attachments/{id}")
  fun deleteAttachment(@Path("id") attachmentId: Int): Call<Any>

}