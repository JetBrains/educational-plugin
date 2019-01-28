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
import com.intellij.util.PlatformUtils
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.net.ssl.CertificateManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.learning.stepik.*
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.apache.http.HttpStatus
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

object StepikConnector {
  private val LOG = Logger.getInstance(StepikConnector::class.java)
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper

  init {
    val module = SimpleModule()
    module.addDeserializer(Lesson::class.java, JacksonLessonDeserializer())
    module.addDeserializer(StepOptions::class.java, JacksonStepOptionsDeserializer())
    module.addDeserializer(Reply::class.java, StepikReplyDeserializer())
    objectMapper = createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  fun createMapper(module: SimpleModule): ObjectMapper {
    val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    objectMapper.addMixIn(EduCourse::class.java, StepikEduCourseMixin::class.java)
    objectMapper.addMixIn(Section::class.java, StepikSectionMixin::class.java)
    objectMapper.addMixIn(Lesson::class.java, StepikLessonMixin::class.java)
    objectMapper.addMixIn(TaskFile::class.java, StepikTaskFileMixin::class.java)
    objectMapper.addMixIn(Task::class.java, StepikTaskMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholder::class.java, StepikAnswerPlaceholderMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, StepikAnswerPlaceholderDependencyMixin::class.java)
    objectMapper.addMixIn(FeedbackLink::class.java, StepikFeedbackLinkMixin::class.java)
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    objectMapper.registerModule(module)
    return objectMapper
  }

  private val authorizationService: StepikOAuthService = Retrofit.Builder()
    .baseUrl(StepikNames.STEPIK_URL)
    .addConverterFactory(converterFactory)
    .build().create(StepikOAuthService::class.java)

  internal val service: StepikService
    get() = service(EduSettings.getInstance().user)

  private fun service(account: StepikUser?): StepikService {
    if (account != null && !account.tokenInfo.isUpToDate()) {
      account.refreshTokens()
    }

    val dispatcher = Dispatcher()
    dispatcher.maxRequests = 10

    val builder = OkHttpClient.Builder()
      .readTimeout(60, TimeUnit.SECONDS)
      .connectTimeout(60, TimeUnit.SECONDS)
      .addInterceptor { chain ->
        val builder = chain.request().newBuilder().addHeader("User-Agent", getUserAgent())
        val tokenInfo = account?.tokenInfo
        if (tokenInfo != null) {
          builder.addHeader("Authorization", "Bearer ${tokenInfo.accessToken}")
        }
        val newRequest = builder.build()
        chain.proceed(newRequest)
      }
      .dispatcher(dispatcher)

    val proxyConfigurable = HttpConfigurable.getInstance()
    val proxies = proxyConfigurable.onlyBySettingsSelector.select(URI.create(StepikNames.STEPIK_URL))
    val address = if (proxies.size > 0) proxies[0].address() as? InetSocketAddress else null
    if (address != null) {
      builder.proxy(Proxy(Proxy.Type.HTTP, address))
    }
    val trustManager = CertificateManager.getInstance().trustManager
    val sslContext = CertificateManager.getInstance().sslContext
    builder.sslSocketFactory(sslContext.socketFactory, trustManager)

    val okHttpClient = builder.build()

    val retrofit = Retrofit.Builder()
      .baseUrl(StepikNames.STEPIK_API_URL)
      .addConverterFactory(converterFactory)
      .client(okHttpClient)
      .build()

    return retrofit.create(StepikService::class.java)
  }

  // Authorization requests:

  private fun StepikUser.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val response = authorizationService.refreshTokens("refresh_token", StepikNames.CLIENT_ID, refreshToken).execute()
    checkForErrors(response)
    val tokens = response.body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  fun login(code: String, redirectUri: String): Boolean {
    val response = authorizationService.getTokens(
      StepikNames.CLIENT_ID, redirectUri, code, "authorization_code").execute()
    checkForErrors(response)
    val tokenInfo = response.body() ?: return false
    val stepikUser = StepikUser(tokenInfo)
    val stepikUserInfo = getCurrentUserInfo(stepikUser) ?: return false
    stepikUser.userInfo = stepikUserInfo
    EduSettings.getInstance().user = stepikUser
    return true
  }

  // Get requests:

  fun getCurrentUserInfo(stepikUser: StepikUser): StepikUserInfo? {
    val response = service(stepikUser).getCurrentUser().execute()
    checkForErrors(response)
    return response.body()?.users?.firstOrNull()
  }

  fun isEnrolledToCourse(courseId: Int, stepikUser: StepikUser): Boolean {
    val response = service(stepikUser).enrollments(courseId).execute()
    checkForErrors(response)
    return response.code() == HttpStatus.SC_OK
  }

  fun getCourses(isPublic: Boolean, currentPage: Int, enrolled: Boolean?): CoursesList? {
    val response = service.courses(true, isPublic, currentPage, enrolled).execute()
    checkForErrors(response)
    return response.body()
  }

  @JvmOverloads
  fun getCourseInfo(courseId: Int, isIdeaCompatible: Boolean? = null): EduCourse? {
    val response = service.courses(courseId, isIdeaCompatible).execute()
    checkForErrors(response)
    val course = response.body()?.courses?.firstOrNull()
    if (course != null) {
      setCourseLanguage(course)
    }
    return course
  }

  fun getSection(sectionId: Int): Section? {
    val response = service.sections(sectionId).execute()
    checkForErrors(response)
    return response.body()?.sections?.firstOrNull()
  }

  fun getLesson(lessonId: Int): Lesson? {
    val response = service.lessons(lessonId).execute()
    checkForErrors(response)
    return response.body()?.lessons?.firstOrNull()
  }

  fun getUnit(unitId: Int): StepikUnit? {
    val response = service.units(unitId).execute()
    checkForErrors(response)
    return response.body()?.units?.firstOrNull()
  }

  fun getLessonUnit(lessonId: Int): StepikUnit? {
    val response = service.lessonUnit(lessonId).execute()
    checkForErrors(response)
    return response.body()?.units?.firstOrNull()
  }

  fun getStep(stepId: Int): StepSource? {
    val response = service.steps(stepId).execute()
    checkForErrors(response)
    return response.body()?.steps?.firstOrNull()
  }

  fun getSubmissions(isSolved: Boolean, stepId: Int): List<Submission>? {
    val response = service.submissions(status = if (isSolved) "correct" else "wrong", step = stepId).execute()
    checkForErrors(response)
    return response.body()?.submissions
  }

  fun getSubmissions(attemptId: Int, userId: Int): List<Submission>? {
    val response = service.submissions(attempt = attemptId, user = userId).execute()
    checkForErrors(response)
    return response.body()?.submissions
  }

  fun getLastSubmission(stepId: Int, isSolved: Boolean): Reply? {
    val submissions = getSubmissions(isSolved, stepId)
    return submissions?.firstOrNull()?.reply
  }

  fun getAttempts(stepId: Int, userId: Int): List<Attempt>? {
    val response = service.attempts(stepId, userId).execute()
    checkForErrors(response)
    return response.body()?.attempts
  }

  // Post requests:

  fun postCourse(course: EduCourse): EduCourse? {
    val response = service.course(CourseData(course)).execute()
    checkForErrors(response)
    return response.body()?.courses?.firstOrNull()
  }

  fun postSection(section: Section): Section? {
    val response = service.section(SectionData(section)).execute()
    checkForErrors(response)
    return response.body()?.sections?.firstOrNull()
  }

  fun postLesson(lesson: Lesson): Lesson? {
    val response = service.lesson(LessonData(lesson)).execute()
    checkForErrors(response)
    return response.body()?.lessons?.firstOrNull()
  }

  fun postUnit(lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = service.unit(UnitData(lessonId, position, sectionId)).execute()
    checkForErrors(response)
    return response.body()?.units?.firstOrNull()
  }

  fun postTask(project: Project, task: Task, lessonId: Int): StepSource? {
    var stepSourceData: StepSourceData? = null
    invokeAndWaitIfNeed {
      FileDocumentManager.getInstance().saveAllDocuments()
      stepSourceData = StepSourceData(project, task, lessonId)
    }
    val response = service.stepSource(stepSourceData!!).execute()
    checkForErrors(response)
    return response.body()?.steps?.firstOrNull()
  }

  fun postSubmission(passed: Boolean, attempt: Attempt,
                     files: ArrayList<SolutionFile>, task: Task): List<Submission>? {
    return postSubmission(SubmissionData(attempt.id, if (passed) "1" else "0", files, task))
  }

  fun postSubmission(submissionData: SubmissionData): List<Submission>? {
    val response = service.submission(submissionData).execute()
    checkForErrors(response)
    val submissions = response.body()?.submissions
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to make submission $submissions")
      return null
    }
    return submissions
  }

  fun postAttempt(id: Int): Attempt? {
    val response = service.attempt(AttemptData(id)).execute()
    checkForErrors(response)
    val attempt = response.body()?.attempts?.firstOrNull()
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Failed to make attempt $id")
      return null
    }
    return attempt
  }

  fun postView(assignmentId: Int, stepId: Int) {
    val response = service.view(ViewData(assignmentId, stepId)).execute()
    checkForErrors(response)
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Error while Views post, code: " + response.code())
    }
  }

  fun postMember(userId: String, group: String): Int {
    val response = service.members(MemberData(userId, group)).execute()
    checkForErrors(response)
    return response.code()
  }

  fun enrollToCourse(courseId: Int, stepikUser: StepikUser) {
    val response = service(stepikUser).enrollment(EnrollmentData(courseId)).execute()
    checkForErrors(response)
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to enroll user ${stepikUser.id} to course $courseId")
    }
  }

  // Update requests:

  fun updateCourse(course: EduCourse): Int {
    val response = service.course(course.id, CourseData(course)).execute()
    checkForErrors(response)
    return response.code()
  }

  fun updateSection(section: Section): Section? {
    val response = service.section(section.id, SectionData(section)).execute()
    checkForErrors(response)
    return response.body()?.sections?.firstOrNull()
  }

  fun updateLesson(lesson: Lesson): Lesson? {
    val response = service.lesson(lesson.id, LessonData(lesson)).execute()
    checkForErrors(response)
    return response.body()?.lessons?.firstOrNull()
  }

  fun updateUnit(unitId: Int, lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = service.unit(unitId, UnitData(lessonId, position, sectionId, unitId)).execute()
    checkForErrors(response)
    return response.body()?.units?.firstOrNull()
  }

  fun updateTask(project: Project, task: Task): Int {
    var stepSourceData: StepSourceData? = null
    invokeAndWaitIfNeed {
      FileDocumentManager.getInstance().saveAllDocuments()
      stepSourceData = StepSourceData(project, task, task.lesson.id)
    }
    val response = service.stepSource(task.stepId, stepSourceData!!).execute()
    checkForErrors(response)
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

  private fun checkForErrors(response: Response<out Any>) {
    val errorBody = response.errorBody()
    if (errorBody != null) {
      LOG.error(errorBody.string())
    }
  }

}

private fun getUserAgent(): String {
  val version = pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"

  return String.format("%s/version(%s)/%s/%s", StepikNames.PLUGIN_NAME, version, System.getProperty("os.name"),
                       PlatformUtils.getPlatformPrefix())
}
