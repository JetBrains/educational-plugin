package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.http.HttpStatus
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*

object StepikConnector {
  private val LOG = Logger.getInstance(StepikConnector::class.java)
  private val converterFactory: JacksonConverterFactory
  @JvmStatic
  val objectMapper: ObjectMapper

  init {
    val module = SimpleModule()
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
    objectMapper.addMixIn(ChoiceTask::class.java, StepikChoiceTaskMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholder::class.java, StepikAnswerPlaceholderMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, StepikAnswerPlaceholderDependencyMixin::class.java)
    objectMapper.addMixIn(FeedbackLink::class.java, StepikFeedbackLinkMixin::class.java)
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    objectMapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_SETTERS)
    objectMapper.registerModule(module)
    return objectMapper
  }

  private val authorizationService: StepikOAuthService = createRetrofitBuilder(StepikNames.STEPIK_URL)
      .addConverterFactory(converterFactory)
      .build()
      .create(StepikOAuthService::class.java)

  internal val service: StepikService
    get() = service(EduSettings.getInstance().user)

  private fun service(account: StepikUser?): StepikService {
    if (account != null && !account.tokenInfo.isUpToDate()) {
      account.refreshTokens()
    }

    return createRetrofitBuilder(StepikNames.STEPIK_URL, account?.tokenInfo?.accessToken)
      .addConverterFactory(converterFactory)
      .build()
      .create(StepikService::class.java)
  }

  // Authorization requests:

  private fun StepikUser.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val response = authorizationService.refreshTokens("refresh_token", StepikNames.CLIENT_ID, refreshToken).executeHandlingExceptions()
    val tokens = response?.body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  @JvmStatic
  fun login(code: String, redirectUri: String): Boolean {
    val response = authorizationService.getTokens(
      StepikNames.CLIENT_ID, redirectUri, code, "authorization_code").executeHandlingExceptions()
    val tokenInfo = response?.body() ?: return false
    val stepikUser = StepikUser(tokenInfo)
    val stepikUserInfo = getCurrentUserInfo(stepikUser) ?: return false
    stepikUser.userInfo = stepikUserInfo
    EduSettings.getInstance().user = stepikUser
    return true
  }

  // Get requests:

  @JvmStatic
  fun getCurrentUserInfo(stepikUser: StepikUser): StepikUserInfo? {
    val response = service(stepikUser).getCurrentUser().executeHandlingExceptions()
    return response?.body()?.users?.firstOrNull()
  }

  @JvmStatic
  fun isEnrolledToCourse(courseId: Int, stepikUser: StepikUser): Boolean {
    val response = service(stepikUser).enrollments(courseId).executeHandlingExceptions(true)
    return response?.code() == HttpStatus.SC_OK
  }

  @JvmStatic
  fun getCourses(isPublic: Boolean, currentPage: Int, enrolled: Boolean?): CoursesList? {
    val response = service.courses(true, isPublic, currentPage, enrolled).executeHandlingExceptions()
    return response?.body()
  }

  @JvmOverloads
  @JvmStatic
  fun getCourseInfo(courseId: Int, isIdeaCompatible: Boolean? = null, optional: Boolean = false): EduCourse? {
    val response = service.courses(courseId, isIdeaCompatible).executeHandlingExceptions(optional)
    return response?.body()?.courses?.firstOrNull()?.apply { setCourseLanguage(this) }
  }

  @JvmStatic
  fun getSection(sectionId: Int): Section? {
    val response = service.sections(sectionId).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  @JvmStatic
  fun getLesson(lessonId: Int): Lesson? {
    val response = service.lessons(lessonId).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  @JvmStatic
  fun getUnit(unitId: Int): StepikUnit? {
    val response = service.units(unitId).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  @JvmStatic
  fun getLessonUnit(lessonId: Int): StepikUnit? {
    val response = service.lessonUnit(lessonId).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  @JvmStatic
  fun getStep(stepId: Int): StepSource? {
    val response = service.steps(stepId).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  @JvmStatic
  fun getSubmissions(isSolved: Boolean, stepId: Int): List<Submission>? {
    val response = service.submissions(status = if (isSolved) "correct" else "wrong", step = stepId).executeHandlingExceptions()
    return response?.body()?.submissions
  }

  @JvmStatic
  fun getSubmissions(attemptId: Int, userId: Int): List<Submission>? {
    val response = service.submissions(attempt = attemptId, user = userId).executeHandlingExceptions()
    return response?.body()?.submissions
  }

  @JvmStatic
  fun getLastSubmission(stepId: Int, isSolved: Boolean): Reply? {
    val submissions = getSubmissions(isSolved, stepId)
    return submissions?.firstOrNull()?.reply
  }

  @JvmStatic
  fun getAttempts(stepId: Int, userId: Int): List<Attempt>? {
    val response = service.attempts(stepId, userId).executeHandlingExceptions()
    return response?.body()?.attempts
  }

  // Post requests:

  @JvmStatic
  fun postCourse(course: EduCourse): EduCourse? {
    val response = service.course(CourseData(course)).executeHandlingExceptions()
    return response?.body()?.courses?.firstOrNull()
  }

  @JvmStatic
  fun postSection(section: Section): Section? {
    val response = service.section(SectionData(section)).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  @JvmStatic
  fun postLesson(lesson: Lesson): Lesson? {
    val response = service.lesson(LessonData(lesson)).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  @JvmStatic
  fun postUnit(lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = service.unit(UnitData(lessonId, position, sectionId)).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  @JvmStatic
  fun postTask(project: Project, task: Task, lessonId: Int): StepSource? {
    var stepSourceData: StepSourceData? = null
    // BACKCOMPAT: 2018.3
    @Suppress("DEPRECATION")
    invokeAndWaitIfNeed {
      FileDocumentManager.getInstance().saveAllDocuments()
      stepSourceData = StepSourceData(project, task, lessonId)
    }
    val response = service.stepSource(stepSourceData!!).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  @JvmStatic
  fun postSubmission(passed: Boolean, attempt: Attempt,
                     files: ArrayList<SolutionFile>, task: Task): List<Submission>? {
    return postSubmission(SubmissionData(attempt.id, if (passed) "1" else "0", files, task))
  }

  @JvmStatic
  fun postSubmission(submissionData: SubmissionData): List<Submission>? {
    val response = service.submission(submissionData).executeHandlingExceptions()
    val submissions = response?.body()?.submissions
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to make submission $submissions")
      return null
    }
    return submissions
  }

  @JvmStatic
  fun postAttempt(id: Int): Attempt? {
    val response = service.attempt(AttemptData(id)).executeHandlingExceptions()
    val attempt = response?.body()?.attempts?.firstOrNull()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Failed to make attempt $id")
      return null
    }
    return attempt
  }

  @JvmStatic
  fun postView(assignmentId: Int, stepId: Int) {
    val response = service.view(ViewData(assignmentId, stepId)).executeHandlingExceptions()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Error while Views post, code: " + response?.code())
    }
  }

  @JvmStatic
  fun postMember(userId: String, group: String): Int {
    val response = service.members(MemberData(userId, group)).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  @JvmStatic
  fun enrollToCourse(courseId: Int, stepikUser: StepikUser) {
    val response = service(stepikUser).enrollment(EnrollmentData(courseId)).executeHandlingExceptions()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to enroll user ${stepikUser.id} to course $courseId")
    }
  }

  @JvmStatic
  fun postAttachment(additionalFiles: List<TaskFile>, courseId: Int): Int {
    val additionalInfo = AdditionalInfo(additionalFiles)
    val fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), objectMapper.writeValueAsString(additionalInfo))
    val fileData = MultipartBody.Part.createFormData("file", StepikNames.ADDITIONAL_FILES, fileBody)
    val courseBody = RequestBody.create(MediaType.parse("text/plain"), courseId.toString())

    val response = service.attachment(fileData, courseBody).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  // Update requests:

  @JvmStatic
  fun updateCourse(course: EduCourse): Int {
    val response = service.course(course.id, CourseData(course)).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  @JvmStatic
  fun updateSection(section: Section): Section? {
    val response = service.section(section.id, SectionData(section)).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  @JvmStatic
  fun updateLesson(lesson: Lesson): Lesson? {
    val response = service.lesson(lesson.id, LessonData(lesson)).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  @JvmStatic
  fun updateUnit(unitId: Int, lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = service.unit(unitId, UnitData(lessonId, position, sectionId, unitId)).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  @JvmStatic
  fun updateTask(project: Project, task: Task): Int {
    var stepSourceData: StepSourceData? = null
    // BACKCOMPAT: 2018.3
    @Suppress("DEPRECATION")
    invokeAndWaitIfNeed {
      FileDocumentManager.getInstance().saveAllDocuments()
      stepSourceData = StepSourceData(project, task, task.lesson.id)
    }
    val response = service.stepSource(task.id, stepSourceData!!).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  @JvmStatic
  fun updateAttachment(additionalFiles: List<TaskFile>, course: EduCourse): Int {
    val attachments = service.attachments(course.id).executeHandlingExceptions(true)?.body()
    if (attachments != null && attachments.attachments.isNotEmpty()) {
      val attachmentId = attachments.attachments.firstOrNull { StepikNames.ADDITIONAL_FILES == it.name }?.id
      if (attachmentId != null) {
        service.deleteAttachment(attachmentId).executeHandlingExceptions()
      }
    }
    updateCourse(course)  // Needed to push forward update_date in course
    course.setUpdated()
    return postAttachment(additionalFiles, course.id)
  }

  // Delete requests:

  @JvmStatic
  fun deleteSection(sectionId: Int) {
    service.deleteSection(sectionId).executeHandlingExceptions(true)
  }

  @JvmStatic
  fun deleteLesson(lessonId: Int) {
    service.deleteLesson(lessonId).executeHandlingExceptions(true)
  }

  @JvmStatic
  fun deleteUnit(unitId: Int) {
    service.deleteUnit(unitId).executeHandlingExceptions(true)
  }

  @JvmStatic
  fun deleteTask(taskId: Int) {
    service.deleteStepSource(taskId).executeHandlingExceptions(true)
  }
}
