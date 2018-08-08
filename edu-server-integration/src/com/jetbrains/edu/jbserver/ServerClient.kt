package com.jetbrains.edu.jbserver

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.*
import retrofit2.converter.jackson.JacksonConverterFactory

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor


fun ObjectMapper.setupMapper() = apply {
  addMixIn(StudyItem::class.java, StudyItemMixin::class.java)
  addMixIn(Course::class.java, CourseMixin::class.java)
  addMixIn(Section::class.java, SectionMixin::class.java)
  addMixIn(Lesson::class.java, LessonMixin::class.java)
  addMixIn(Task::class.java, TaskMixIn::class.java)
  addMixIn(TheoryTask::class.java, TheoryTaskMixIn::class.java)
  addMixIn(IdeTask::class.java, IdeTaskMixIn::class.java)
  addMixIn(OutputTask::class.java, OutputTaskMixIn::class.java)
  addMixIn(EduTask::class.java, EduTaskMixIn::class.java)
  addMixIn(CodeTask::class.java, CodeTaskMixIn::class.java)
  addMixIn(ChoiceTask::class.java, ChoiceTaskMixIn::class.java)
  addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
  addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
  addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
  disable(MapperFeature.AUTO_DETECT_CREATORS)
  disable(MapperFeature.AUTO_DETECT_FIELDS)
  disable(MapperFeature.AUTO_DETECT_GETTERS)
  disable(MapperFeature.AUTO_DETECT_SETTERS)
  disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
  setSerializationInclusion(JsonInclude.Include.NON_NULL)
}


interface EduServerApi {

  companion object {

    fun create(): EduServerApi {

      val mapper = jacksonObjectMapper()
        .setupMapper()

      val interceptor = HttpLoggingInterceptor()
        .setLevel(HttpLoggingInterceptor.Level.NONE)

      val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()

      val retrofit = Retrofit.Builder()
        .baseUrl("http://django-edu-server.herokuapp.com")
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .client(client)
        .build()

      return retrofit.create(EduServerApi::class.java)
    }

  }

  @GET("/courses")
  fun getCourses(): Call<CourseList>

  @GET("/courses/{pk}/materials")
  fun getCourseMaterials(@Path("pk") pk: Int): Call<Course>


  @GET("/sections/{pks}")
  fun getSections(@Path("pks") pks: String): Call<SectionList>

  @GET("/lessons/{pks}")
  fun getLessons(@Path("pks") pks: String): Call<LessonList>

  @GET("/tasks/{pks}")
  fun getTasks(@Path("pks") pks: String): Call<TaskList>

}


fun List<Int>.pks() = joinToString(separator = "&")

