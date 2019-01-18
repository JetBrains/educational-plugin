@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import com.google.gson.GsonBuilder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikSteps
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.StepikWrappers
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import java.util.*

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
  fun submissions(@Query("order") order: String="desc",
                  @Query("page") page: Int=1,
                  @Query("status") status: String,
                  @Query("step") step: Int): Call<SubmissionsList>

  @POST("submissions")
  fun submissions(@Body submissionData: SubmissionData): Call<ResponseBody>

  @POST("attempts")
  fun attempts(@Body attemptData: AttemptData): Call<AttemptsList>

  @GET("assignments")
  fun assignments(@Query("ids[]") vararg ids: Int): Call<AssignmentsList>

  @POST("views")
  fun view(@Body viewData: ViewData): Call<ResponseBody>

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

class SectionsList {
  lateinit var sections: List<Section>
}

class LessonsList {
  lateinit var lessons: List<Lesson>
}

class UnitsList {
  lateinit var units: List<StepikWrappers.Unit>
}

class StepsList {
  lateinit var steps: List<StepikSteps.StepSource>
}

class SubmissionsList {
  lateinit var submissions: List<StepikWrappers.Submission>
}

class SubmissionData(attemptId: Int, score: String, files: ArrayList<StepikWrappers.SolutionFile>, task: Task) {
  var submission: StepikWrappers.Submission

  init {
    val serializedTask = GsonBuilder()   // TODO: use jackson
      .excludeFieldsWithoutExposeAnnotation()
      .registerTypeAdapter(Task::class.java, StepikSubmissionTaskAdapter())
      .create()
      .toJson(StepikWrappers.TaskWrapper(task))
    submission = StepikWrappers.Submission(score, attemptId, files, serializedTask)
  }
}

class ProgressesList {
  lateinit var progresses: List<Progress>
}

class Progress {
  lateinit var id: String
  var isPassed: Boolean = false
}

@Suppress("ConvertSecondaryConstructorToPrimary")
class AttemptData {
  var attempt: StepikWrappers.Attempt? = null

  constructor(step: Int) {
    attempt = StepikWrappers.Attempt(step)
  }
}

class AttemptsList {
  lateinit var attempts: List<StepikWrappers.Attempt>
}

class AssignmentsList {
  lateinit var assignments: List<StepikWrappers.Assignment>
}

class ViewData(assignment: Int, step: Int) {
  var view: StepikWrappers.View = StepikWrappers.View(assignment, step)
}
