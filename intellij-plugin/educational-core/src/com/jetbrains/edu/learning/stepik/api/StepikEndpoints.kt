package com.jetbrains.edu.learning.stepik.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface StepikEndpoints {

  // GET requests:

  @GET("api/stepics/1/")
  fun getCurrentUser(): Call<UsersList>

  @GET("api/courses/{id}")
  fun courses(@Path("id") courseId: Int,
              @Query("is_idea_compatible") isIdeaCompatible: Boolean?): Call<CoursesList>

  @GET("api/lessons")
  fun lessons(@Query("ids[]") vararg ids: Int): Call<LessonsList>

  @GET("api/steps")
  fun steps(@Query("ids[]") vararg ids: Int): Call<StepsList>

  @GET("api/attachments")
  fun attachments(@Query("lesson") lessonId: Int? = null): Call<AttachmentsList>

  @GET("api/step-sources/{id}")
  fun choiceStepSource(@Path("id") stepId: Int): Call<ChoiceStepSourcesList>

  // POST requests:

  @POST("api/lessons")
  fun lesson(@Body lessonData: LessonData): Call<LessonsList>

  @POST("api/units")
  fun unit(@Body unitData: UnitData): Call<UnitsList>

  @POST("api/step-sources")
  fun stepSource(@Body stepSourceData: StepSourceData): Call<StepSourcesList>

  @Multipart
  @POST("api/attachments")
  fun attachment(@Part file: MultipartBody.Part, @Part("lesson") lesson: RequestBody? = null): Call<ResponseBody>

  // PUT requests:

  @PUT("api/lessons/{id}")
  fun lesson(@Path("id") lessonId: Int, @Body lessonData: LessonData): Call<LessonsList>

  @PUT("api/units/{id}")
  fun unit(@Path("id") unitId: Int, @Body unitData: UnitData): Call<UnitsList>

  @PUT("api/step-sources/{id}")
  fun stepSource(@Path("id") stepSourceId: Int, @Body stepSourceData: StepSourceData): Call<StepSourcesList>

  // DELETE requests:

  @DELETE("api/step-sources/{id}")
  fun deleteStepSource(@Path("id") taskId: Int): Call<Any>

  @DELETE("api/attachments/{id}")
  fun deleteAttachment(@Path("id") attachmentId: Int): Call<Any>

}
