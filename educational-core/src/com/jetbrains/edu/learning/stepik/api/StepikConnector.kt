package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.*
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.apache.http.HttpStatus
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

object StepikConnector {
  private val LOG = Logger.getInstance(StepikConnector::class.java)
  private val converterFactory: JacksonConverterFactory

  init {
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  val objectMapper: ObjectMapper
    get() {
      val module = SimpleModule()
      val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
      objectMapper.addMixIn(EduCourse::class.java, StepikEduCourseMixin::class.java)
      objectMapper.addMixIn(Section::class.java, StepikSectionMixin::class.java)
      objectMapper.addMixIn(Lesson::class.java, StepikLessonMixin::class.java)
      objectMapper.addMixIn(TaskFile::class.java, StepikTaskFileMixin::class.java)
      objectMapper.addMixIn(AnswerPlaceholder::class.java, StepikAnswerPlaceholderMixin::class.java)
      objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, StepikAnswerPlaceholderDependencyMixin::class.java)
      objectMapper.addMixIn(FeedbackLink::class.java, StepikFeedbackLinkMixin::class.java)
      objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
      objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
      objectMapper.registerModule(module)
      return objectMapper
    }

  private val authorizationService: StepikOAuthService
    get() {
      val retrofit = Retrofit.Builder()
        .baseUrl(StepikNames.STEPIK_URL)
        .addConverterFactory(converterFactory)
        .build()

      return retrofit.create(StepikOAuthService::class.java)
    }

  internal val service: StepikService
    get() = service(EduSettings.getInstance().user)

  private fun service(account: StepikUser?): StepikService {
    if (account != null && !account.tokenInfo.isUpToDate()) {
      account.refreshTokens()
    }

    val dispatcher = Dispatcher()
    dispatcher.maxRequests = 10

    val okHttpClient = OkHttpClient.Builder()
      .readTimeout(60, TimeUnit.SECONDS)
      .connectTimeout(60, TimeUnit.SECONDS)
      .addInterceptor { chain ->
        val tokenInfo = account?.tokenInfo
        if (tokenInfo == null) return@addInterceptor chain.proceed(chain.request())

        val newRequest = chain.request().newBuilder()
          .addHeader("Authorization", "Bearer ${tokenInfo.accessToken}")
          .build()
        chain.proceed(newRequest)
      }
      .dispatcher(dispatcher)
      .build()

    val retrofit = Retrofit.Builder()
      .baseUrl(StepikNames.STEPIK_API_URL_SLASH)
      .addConverterFactory(converterFactory)
      .client(okHttpClient)
      .build()

    return retrofit.create(StepikService::class.java)
  }

  // Authorization requests:

  private fun StepikUser.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val tokens = authorizationService.refreshTokens("refresh_token", StepikNames.CLIENT_ID, refreshToken).execute().body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  fun login(code: String, redirectUri: String): Boolean {
    val tokenInfo = authorizationService.getTokens(StepikNames.CLIENT_ID, redirectUri,
                                                   code, "authorization_code").execute().body() ?: return false
    val stepikUser = StepikUser(tokenInfo)
    val stepikUserInfo = getCurrentUserInfo(stepikUser) ?: return false
    stepikUser.userInfo = stepikUserInfo
    EduSettings.getInstance().user = stepikUser
    return true
  }

  // Get requests:

  fun getCurrentUserInfo(stepikUser: StepikUser): StepikUserInfo? {
    return service(stepikUser).getCurrentUser().execute().body()?.users?.firstOrNull()
  }

  fun isEnrolledToCourse(courseId: Int, stepikUser: StepikUser): Boolean {
    val response = service(stepikUser).enrollments(courseId).execute()
    return response.code() == HttpStatus.SC_OK
  }

  fun getCourses(isPublic: Boolean, currentPage: Int, enrolled: Boolean?) =
    service.courses(true, isPublic, currentPage, enrolled).execute().body()

  fun getCourseInfo(courseId: Int, isIdeaCompatible: Boolean?): EduCourse? {
    val course = service.courses(courseId, isIdeaCompatible).execute().body()?.courses?.firstOrNull()
    if (course != null) {
      setCourseLanguage(course)
    }
    return course
  }

  fun getSection(sectionId: Int): Section? {
    return service.sections(sectionId).execute().body()?.sections?.firstOrNull()
  }

  fun getLesson(lessonId: Int): Lesson? {
    return service.lessons(lessonId).execute().body()?.lessons?.firstOrNull()
  }

  fun getUnit(unitId: Int): StepikWrappers.Unit? {
    return service.units(unitId).execute().body()?.units?.firstOrNull()
  }

  fun getLessonUnit(lessonId: Int): StepikWrappers.Unit? {
    return service.lessonUnit(lessonId).execute().body()?.units?.firstOrNull()
  }

  fun getStep(stepId: Int): StepSource? {
    return service.steps(stepId).execute().body()?.steps?.firstOrNull()
  }

  fun getSubmissions(isSolved: Boolean, stepId: Int) =
    service.submissions(status = if (isSolved) "correct" else "wrong", step = stepId).execute().body()?.submissions

  fun getSubmissions(attemptId: Int, userId: Int) =
    service.submissions(attempt = attemptId, user = userId).execute().body()?.submissions

  fun getLastSubmission(stepId: Int, isSolved: Boolean, language: String): StepikWrappers.Reply? {
    // TODO: make use of language
    val submissions = getSubmissions(isSolved, stepId)
    return submissions?.firstOrNull()?.reply
  }

  fun getAttempts(stepId: Int, userId: Int): List<Attempt>? {
    return service.attempts(stepId, userId).execute().body()?.attempts
  }

  // Post requests:

  fun postCourse(course: Course): EduCourse? {
    return service.course(CourseData(course)).execute().body()?.courses?.firstOrNull()
  }

  fun postSection(section: Section): Section? {
    return service.section(SectionData(section)).execute().body()?.sections?.firstOrNull()
  }

  fun postLesson(lesson: Lesson): Lesson? {
    return service.lesson(LessonData(lesson)).execute().body()?.lessons?.firstOrNull()
  }

  fun postUnit(lessonId: Int, position: Int, sectionId: Int): StepikWrappers.Unit? {
    return service.unit(UnitData(lessonId, position, sectionId)).execute().body()?.units?.firstOrNull()
  }

  fun postTask(project: Project, task: Task, lessonId: Int): StepSource? {
    var stepSourceData: StepSourceData? = null
    invokeAndWaitIfNeed {
      FileDocumentManager.getInstance().saveAllDocuments()
      stepSourceData = StepSourceData(project, task, lessonId)
    }
    return service.stepSource(stepSourceData!!).execute().body()?.steps?.firstOrNull()
  }

  fun postSubmission(passed: Boolean, attempt: Attempt,
                     files: ArrayList<StepikWrappers.SolutionFile>, task: Task): List<StepikWrappers.Submission>? {
    return postSubmission(SubmissionData(attempt.id, if (passed) "1" else "0", files, task))
  }

  fun postSubmission(submissionData: SubmissionData): List<StepikWrappers.Submission>? {
    val response = service.submission(submissionData).execute()
    val submissions = response.body()?.submissions
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to make submission $submissions")
      return null
    }
    return submissions
  }

  fun postAttempt(id: Int): Attempt? {
    val response = service.attempt(AttemptData(id)).execute()
    val attempt = response.body()?.attempts?.firstOrNull()
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Failed to make attempt $id")
      return null
    }
    return attempt
  }

  fun postView(assignmentId: Int, stepId: Int) {
    val response = service.view(ViewData(assignmentId, stepId)).execute()
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Error while Views post, code: " + response.code())
    }
  }

  fun postMember(userId: String, group: String): Int {
    val response = service.members(MemberData(userId, group)).execute()
    return response.code()
  }

  fun enrollToCourse(courseId: Int, stepikUser: StepikUser) {
    val response = service(stepikUser).enrollment(EnrollmentData(courseId)).execute()
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to enroll user ${stepikUser.id} to course $courseId")
    }
  }

  // Update requests:

  fun updateCourse(course: Course): Int {
    var response = service.course(course.id, CourseData(section)).execute()
    return response.code()
  }

  fun updateSection(section: Section): Section? {
    return service.section(section.id, SectionData(section)).execute().body()?.sections?.firstOrNull()
  }

  fun updateLesson(lesson: Lesson): Lesson? {
    return service.lesson(lesson.id, LessonData(lesson)).execute().body()?.lessons?.firstOrNull()
  }

  fun updateUnit(unitId: Int, lessonId: Int, position: Int, sectionId: Int): StepikWrappers.Unit? {
    return service.unit(unitId, UnitData(lessonId, position, sectionId, unitId)).execute().body()?.units?.firstOrNull()
  }

  fun updateTask(project: Project, task: Task): Int {
    var stepSourceData: StepSourceData? = null
    invokeAndWaitIfNeed {
      FileDocumentManager.getInstance().saveAllDocuments()
      stepSourceData = StepSourceData(project, task, task.lesson.id)
    }
    val response = service.stepSource(task.stepId, stepSourceData!!).execute()
    return response.code()
  }

  // Delete requests:

  fun deleteSection(sectionId: Int) {
    val response = service.deleteSection(sectionId).execute()
    validateDeleteResponse(response.code(), sectionId)
  }

  fun deleteLesson(lessonId: Int) {
    val response = service.deleteLesson(lessonId).execute()
    validateDeleteResponse(response.code(), lessonId)
  }

  fun deleteUnit(unitId: Int) {
    val response = service.deleteUnit(unitId).execute()
    validateDeleteResponse(response.code(), unitId)
  }

  fun deleteTask(taskId: Int) {
    val response = service.deleteStepSource(taskId).execute()
    validateDeleteResponse(response.code(), taskId)
  }

  private fun validateDeleteResponse(responseCode: Int, id: Int) {
    if (responseCode != HttpStatus.SC_NO_CONTENT) {
      // If parent item was deleted its children are deleted too, so it's ok to fail to delete item here
      LOG.warn("Failed to delete item $id")
    }
  }
}
