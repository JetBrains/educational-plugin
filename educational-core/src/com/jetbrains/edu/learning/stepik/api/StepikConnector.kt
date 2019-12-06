package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.stepik.*
import com.jetbrains.edu.learning.stepikUserAgent
import okhttp3.ConnectionPool
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.http.HttpStatus
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*

abstract class StepikConnector {

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper

  init {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    module.addDeserializer(Reply::class.java, StepikReplyDeserializer())
    objectMapper = createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  protected abstract val baseUrl: String

  private val authorizationService: StepikOAuthService
    get() = createRetrofitBuilder(baseUrl, connectionPool, stepikUserAgent)
      .addConverterFactory(converterFactory)
      .build()
      .create(StepikOAuthService::class.java)

  private val service: StepikService
    get() = service(EduSettings.getInstance().user)

  private fun service(account: StepikUser?): StepikService {
    if (account != null && !account.tokenInfo.isUpToDate()) {
      account.refreshTokens()
    }

    return createRetrofitBuilder(baseUrl, connectionPool, stepikUserAgent, account?.tokenInfo?.accessToken)
      .addConverterFactory(converterFactory)
      .build()
      .create(StepikService::class.java)
  }

  // Authorization requests:

  private fun StepikUser.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val response = authorizationService
      .refreshTokens("refresh_token", StepikNames.CLIENT_ID, StepikNames.CLIENT_SECRET, refreshToken).executeHandlingExceptions()
    val tokens = response?.body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  fun login(code: String, redirectUri: String): Boolean {
    val response = authorizationService.getTokens(
      StepikNames.CLIENT_ID, StepikNames.CLIENT_SECRET, redirectUri, code, "authorization_code"
    ).executeHandlingExceptions()
    val tokenInfo = response?.body() ?: return false
    val stepikUser = StepikUser(tokenInfo)
    val stepikUserInfo = getCurrentUserInfo(stepikUser) ?: return false
    stepikUser.userInfo = stepikUserInfo
    EduSettings.getInstance().user = stepikUser
    return true
  }

  // Get requests:

  fun getCurrentUserInfo(stepikUser: StepikUser): StepikUserInfo? {
    val response = service(stepikUser).getCurrentUser().executeHandlingExceptions()
    return response?.body()?.users?.firstOrNull()
  }

  fun isEnrolledToCourse(courseId: Int, stepikUser: StepikUser): Boolean {
    val response = service(stepikUser).enrollments(courseId).executeHandlingExceptions(true)
    return response?.code() == HttpStatus.SC_OK
  }

  fun getCourses(isPublic: Boolean, currentPage: Int, enrolled: Boolean?): CoursesList? {
    val response = service.courses(true, isPublic, currentPage, enrolled).executeHandlingExceptions()
    return response?.body()
  }

  @JvmOverloads
  fun getCourseInfo(courseId: Int, isIdeaCompatible: Boolean? = null, optional: Boolean = false): EduCourse? {
    val response = service.courses(courseId, isIdeaCompatible).executeHandlingExceptions(optional)
    return response?.body()?.courses?.firstOrNull()?.apply { setCourseLanguageEnvironment(this) }
  }

  fun getSection(sectionId: Int): Section? {
    val response = service.sections(sectionId).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  fun getLesson(lessonId: Int): Lesson? {
    val response = service.lessons(lessonId).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  fun getUnit(unitId: Int): StepikUnit? {
    val response = service.units(unitId).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  fun getLessonUnit(lessonId: Int): StepikUnit? {
    val response = service.lessonUnit(lessonId).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  fun getStep(stepId: Int): StepSource? {
    val response = service.steps(stepId).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  fun getChoiceStepSource(stepId: Int): ChoiceStep? {
    val stepSource = service.choiceStepSource(stepId).executeHandlingExceptions(true)?.body()?.steps?.firstOrNull()
    return stepSource?.block
  }

  fun getAllSubmissions(stepId: Int): MutableList<Submission> {
    var currentPage = 1
    val allSubmissions = mutableListOf<Submission>()
    while (true) {
      val submissionsList = getSubmissionsList(stepId, currentPage) ?: break
      val submissions = submissionsList.submissions
      allSubmissions.addAll(submissions)
      if (submissions.isEmpty() || !submissionsList.meta.containsKey("has_next") || submissionsList.meta["has_next"] == false) {
        SubmissionsManager.putToSubmissions(stepId, allSubmissions)
        break
      }
      currentPage += 1
    }
    return allSubmissions
  }

  fun getSubmission(attemptId: Int, userId: Int): Submission? {
    val response = service.submissions(attempt = attemptId, user = userId).executeHandlingExceptions()
    val submissions = response?.body()?.submissions ?: return null
    if (submissions.size != 1) {
      LOG.warn("Got a submission wrapper with incorrect submissions number: " + submissions.size)
    }
    return submissions.firstOrNull()
  }

  private fun getSubmissionsList(stepId: Int, page: Int = 1): SubmissionsList? {
    val response = service.submissions(step = stepId, page = page).executeHandlingExceptions()
    return response?.body()
  }

  fun getAttempts(stepId: Int, userId: Int): List<Attempt>? {
    val response = service.attempts(stepId, userId).executeHandlingExceptions()
    return response?.body()?.attempts
  }

  // Post requests:

  fun postCourse(course: EduCourse): EduCourse? {
    val response = service.course(CourseData(course)).executeHandlingExceptions()
    return response?.body()?.courses?.firstOrNull()
  }

  fun postSection(section: Section): Section? {
    val response = service.section(SectionData(section)).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  fun postLesson(lesson: Lesson): Lesson? {
    val response = service.lesson(LessonData(lesson)).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  fun postUnit(lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = service.unit(UnitData(lessonId, position, sectionId)).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  fun postTask(project: Project, task: Task, lessonId: Int): StepSource? {
    var stepSourceData: StepSourceData? = null
    try {
      invokeAndWaitIfNeeded {
        FileDocumentManager.getInstance().saveAllDocuments()
        stepSourceData = StepSourceData(project, task, lessonId)
      }
    } catch (e: RuntimeException) {
      val cause = e.cause as? BrokenPlaceholderException
      LOG.info("${e.message}\n${cause?.placeholderInfo}")
      return null
    }
    val response = service.stepSource(stepSourceData!!).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  fun postSubmission(passed: Boolean, attempt: Attempt,
                     files: ArrayList<SolutionFile>, task: Task): Submission? {
    return postSubmission(SubmissionData(attempt.id, if (passed) "1" else "0", files, task))
  }

  fun postSubmission(submissionData: SubmissionData): Submission? {
    val response = service.submission(submissionData).executeHandlingExceptions()
    val submissions = response?.body()?.submissions ?: return null
    if (response.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to make submission $submissions")
      return null
    }
    if (submissions.size != 1) {
      LOG.warn("Got a submission wrapper with incorrect submissions number: " + submissions.size)
    }
    return submissions.firstOrNull()
  }

  fun postAttempt(id: Int): Attempt? {
    val response = service.attempt(AttemptData(id)).executeHandlingExceptions()
    val attempt = response?.body()?.attempts?.firstOrNull()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Failed to make attempt $id")
      return null
    }
    return attempt
  }

  fun postView(assignmentId: Int, stepId: Int) {
    val response = service.view(ViewData(assignmentId, stepId)).executeHandlingExceptions()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Error while Views post, code: " + response?.code())
    }
  }

  fun postMember(userId: String, group: String): Int {
    val response = service.members(MemberData(userId, group)).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  fun enrollToCourse(courseId: Int, stepikUser: StepikUser) {
    val response = service(stepikUser).enrollment(EnrollmentData(courseId)).executeHandlingExceptions()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to enroll user ${stepikUser.id} to course $courseId")
    }
  }

  private fun postAttachment(info: AdditionalInfo, courseId: Int?, lessonId: Int?): Int {
    val fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), objectMapper.writeValueAsString(info))
    val fileData = MultipartBody.Part.createFormData("file", StepikNames.ADDITIONAL_INFO, fileBody)
    val courseBody = if (courseId != null) RequestBody.create(MediaType.parse("text/plain"), courseId.toString()) else null
    val lessonBody = if (lessonId != null) RequestBody.create(MediaType.parse("text/plain"), lessonId.toString()) else null

    val response = service.attachment(fileData, courseBody, lessonBody).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  fun postCourseAttachment(info: CourseAdditionalInfo, courseId: Int) = postAttachment(info, courseId, null)

  private fun postLessonAttachment(info: LessonAdditionalInfo, lessonId: Int) = postAttachment(info, null, lessonId)

  // Update requests:

  fun updateCourse(course: Course): Int {
    val response = service.course(course.id, CourseData(course)).executeHandlingExceptions()
    val postedCourse = response?.body()?.courses?.firstOrNull()
    if (postedCourse != null) {
      course.updateDate = postedCourse.updateDate
    }
    return response?.code() ?: -1
  }

  fun updateSection(section: Section): Section? {
    val response = service.section(section.id, SectionData(section)).executeHandlingExceptions()
    val postedSection = response?.body()?.sections?.firstOrNull()
    if (postedSection != null) {
      section.updateDate = postedSection.updateDate
    }
    return postedSection
  }

  fun updateLesson(lesson: Lesson): Lesson? {
    val response = service.lesson(lesson.id, LessonData(lesson)).executeHandlingExceptions()
    val postedLesson = response?.body()?.lessons?.firstOrNull()
    if (postedLesson != null) {
      lesson.updateDate = postedLesson.updateDate
    }
    return postedLesson
  }

  fun updateUnit(unitId: Int, lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = service.unit(unitId, UnitData(lessonId, position, sectionId, unitId)).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  fun updateTask(project: Project, task: Task): Int {
    invokeAndWaitIfNeeded {
      FileDocumentManager.getInstance().saveAllDocuments()
    }
    val stepSourceData = try {
      StepSourceData(project, task, task.lesson.id)
    }
    catch (e: RuntimeException) {
      val cause = e.cause as? BrokenPlaceholderException
      LOG.error("${e.message}\n${cause?.placeholderInfo}")
      return -1
    }
    val response = service.stepSource(task.id, stepSourceData).executeHandlingExceptions()
    val stepSource = response?.body()?.steps?.firstOrNull()
    if (stepSource != null) {
      task.updateDate = stepSource.updateDate
    }
    return response?.code() ?: -1
  }

  fun updateCourseAttachment(info: CourseAdditionalInfo, course: Course): Int {
    deleteAttachment(course.id)
    updateCourse(course)  // Needed to push forward update_date in course
    return postCourseAttachment(info, course.id)
  }

  fun updateLessonAttachment(info: LessonAdditionalInfo, lesson: Lesson): Int {
    deleteAttachment(null, lesson.id)
    updateLesson(lesson) // Needed to push forward update_date in lesson
    return postLessonAttachment(info, lesson.id)
  }

  fun deleteLessonAttachment(lessonId: Int) = deleteAttachment(null, lessonId)

  private fun deleteAttachment(courseId: Int?, lessonId: Int? = null) {
    val attachments = service.attachments(courseId, lessonId).executeHandlingExceptions(true)?.body()
    if (attachments != null && attachments.attachments.isNotEmpty()) {
      val attachmentId = attachments.attachments.firstOrNull { StepikNames.ADDITIONAL_INFO == it.name }?.id
      if (attachmentId != null) {
        service.deleteAttachment(attachmentId).executeHandlingExceptions()
      }
    }
  }

  // Delete requests:

  fun deleteSection(sectionId: Int) {
    service.deleteSection(sectionId).executeHandlingExceptions(true)
  }

  fun deleteLesson(lessonId: Int) {
    service.deleteLesson(lessonId).executeHandlingExceptions(true)
  }

  fun deleteUnit(unitId: Int) {
    service.deleteUnit(unitId).executeHandlingExceptions(true)
  }

  fun deleteTask(taskId: Int) {
    service.deleteStepSource(taskId).executeHandlingExceptions(true)
  }

  // Multiple requests:

  fun getUsers(result: List<EduCourse>): MutableList<StepikUserInfo> {
    val instructorIds = result.flatMap { it.instructors }.distinct().chunked(MAX_REQUEST_PARAMS)
    val allUsers = mutableListOf<StepikUserInfo>()
    instructorIds
      .mapNotNull {
        val response = service.users(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.users
      }
      .forEach { allUsers.addAll(it) }
    return allUsers
  }

  fun getSections(sectionIds: List<Int>): List<Section> {
    val sectionIdsChunks = sectionIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allSections = mutableListOf<Section>()
    sectionIdsChunks
      .mapNotNull {
        val response = service.sections(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.sections
      }
      .forEach { allSections.addAll(it) }
    return allSections
  }

  fun getLessons(lessonIds: List<Int>): List<Lesson> {
    val lessonsIdsChunks = lessonIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allLessons = mutableListOf<Lesson>()
    lessonsIdsChunks
      .mapNotNull {
        val response = service.lessons(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.lessons
      }
      .forEach { allLessons.addAll(it) }
    return allLessons
  }

  fun getUnits(unitIds: List<Int>): List<StepikUnit> {
    val unitsIdsChunks = unitIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allUnits = mutableListOf<StepikUnit>()
    unitsIdsChunks
      .mapNotNull {
        val response = service.units(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.units
      }
      .forEach { allUnits.addAll(it) }
    return allUnits
  }

  fun getAssignments(ids: List<Int>): List<Assignment> {
    val idsChunks = ids.distinct().chunked(MAX_REQUEST_PARAMS)
    val assignments = mutableListOf<Assignment>()
    idsChunks
      .mapNotNull {
        val response = service.assignments(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.assignments
      }
      .forEach { assignments.addAll(it) }

    return assignments
  }

  fun getStepSources(stepIds: List<Int>): List<StepSource> {
    val stepsIdsChunks = stepIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val steps = mutableListOf<StepSource>()
    stepsIdsChunks
      .mapNotNull {
        val response = service.steps(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.steps
      }
      .forEach { steps.addAll(it) }
    return steps
  }

  fun taskStatuses(ids: List<String>): Map<String, Boolean> {
    val idsChunks = ids.distinct().chunked(MAX_REQUEST_PARAMS)
    val progresses = mutableListOf<Progress>()
    idsChunks
      .mapNotNull {
        val response = service.progresses(*it.toTypedArray()).executeHandlingExceptions()
        response?.body()?.progresses
      }
      .forEach { progresses.addAll(it) }

    return progresses.associate { it.id to it.isPassed }
  }

  companion object {
    private const val MAX_REQUEST_PARAMS = 100 // restriction of Stepik API for multiple requests
    private val LOG = Logger.getInstance(StepikConnector::class.java)

    @JvmStatic
    fun getInstance(): StepikConnector = service()

    @JvmStatic
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
  }
}
