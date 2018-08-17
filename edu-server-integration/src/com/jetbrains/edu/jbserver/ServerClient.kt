package com.jetbrains.edu.jbserver

import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.*
import retrofit2.converter.jackson.JacksonConverterFactory

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*


val baseUrl = "http://django-edu-server.herokuapp.com"
val loggingLevel = HttpLoggingInterceptor.Level.NONE


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
  dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSXXX")

  val updateModule = SimpleModule()
  updateModule.addSerializer(Section::class.java, SectionSerializer())
  updateModule.addSerializer(Lesson::class.java, LessonSerializer())
  updateModule.addSerializer(Task::class.java, TaskSerializer())
  registerModule(updateModule)
}


fun List<Int>.pks() = joinToString(separator = "&")


interface EduServerApi {

  companion object {

    fun create(): EduServerApi {
      val mapper = jacksonObjectMapper()
        .setupMapper()
      val interceptor = HttpLoggingInterceptor()
        .setLevel(loggingLevel)
      val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
      val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .client(client)
        .build()
      return retrofit.create(EduServerApi::class.java)
    }

  }

  @GET("/courses")
  fun getCourses(): Call<CourseList>

  @GET("/courses/{pk}/materials")
  fun getCourseMaterials(@Path("pk") pk: Int): Call<EduCourse>

  @GET("/courses/{pk}/structure")
  fun getCourseStructure(@Path("pk") pk: Int): Call<EduCourse>

  @GET("/sections/{pks}")
  fun getSections(@Path("pks") pks: String): Call<SectionList>

  @GET("/lessons/{pks}")
  fun getLessons(@Path("pks") pks: String): Call<LessonList>

  @GET("/tasks/{pks}")
  fun getTasks(@Path("pks") pks: String): Call<TaskList>

  @POST("/courses")
  fun createCourse(@Body course: EduCourse): Call<EduCourse>

  @PUT("/courses/{pk}/update")
  fun updateCourse(@Body course: EduCourse): Call<EduCourse>

}


object ServerClient {

  private val service = EduServerApi.create()

  /* Educator API */

  fun createCourse(course: EduCourse): Unit = TODO()

  fun updateCourse(course: EduCourse): Unit = TODO()


  /* Learner API */

  fun getAvailableCourses(): List<EduCourse> = TODO()

  fun getCourseMaterials(id: Int): EduCourse = TODO()

  fun getCourseUpdate(course: EduCourse): Unit = TODO()

}

