package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.ConnectorUtils
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.authUtils.OAuthUtils.checkBuiltinPortValid
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.*
import com.jetbrains.edu.learning.stepik.StepikNames.getClientId
import com.jetbrains.edu.learning.stepik.StepikNames.getClientSecret
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionData
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.http.HttpStatus
import java.io.BufferedReader
import java.io.IOException
import java.net.URL

abstract class StepikConnector : EduOAuthConnector<StepikUser, StepikUserInfo>(), StepikBasedConnector {
  override val platformName: String = StepikNames.STEPIK

  override val account: StepikUser?
    get() = EduSettings.getInstance().user

  override val authorizationTopicName: String = "Edu.stepikLoggedIn"

  override val clientId: String = getClientId()

  override val clientSecret: String = getClientSecret()

  override val objectMapper: ObjectMapper by lazy {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    module.addDeserializer(Reply::class.java, StepikReplyDeserializer())
    createObjectMapper(module)
  }

  private val stepikEndpoints: StepikEndpoints
    get() = stepikEndpoints(account)

  private fun stepikEndpoints(
    account: StepikUser?,
    accessToken: String? = account?.getAccessToken()
  ): StepikEndpoints {
    return getEndpoints(account, accessToken)
  }

  // Authorization requests:

  fun doAuthorize(vararg postLoginActions: Runnable, ifFailedAction: Runnable? = null) {
    if (!checkBuiltinPortValid()) return

    initiateAuthorizationListener(*postLoginActions)

    val redirectUrl = StepikAuthorizer.getOAuthRedirectUrl()
    val link = StepikAuthorizer.createOAuthLink(redirectUrl)
    BrowserUtil.browse(link)

    if (ifFailedAction != null && !redirectUrl.startsWith("http://localhost")) {
      ifFailedAction.run()
    }
  }

  fun login(code: String, redirectUri: String): Boolean {
    val tokenInfo = retrieveLoginToken(code, redirectUri) ?: return false
    val stepikUser = StepikUser(tokenInfo)
    val currentUser = getUserInfo(stepikUser, tokenInfo.accessToken) ?: return false
    if (currentUser.isGuest) {
      // it means that session is broken, so we should force user to re-login
      LOG.warn("User ${currentUser.getFullName()} is anonymous")
      EduSettings.getInstance().user = null
      return false
    }
    stepikUser.userInfo = currentUser
    stepikUser.saveTokens(tokenInfo)
    EduSettings.getInstance().user = stepikUser
    return true
  }

  // Get requests:

  override fun getUserInfo(account: StepikUser, accessToken: String?): StepikUserInfo? {
    val response = stepikEndpoints(account, accessToken).getCurrentUser().executeHandlingExceptions()
    return response?.body()?.users?.firstOrNull()
  }

  fun isEnrolledToCourse(courseId: Int): Boolean {
    val response = stepikEndpoints.enrollments(courseId).executeHandlingExceptions(true)
    return response?.code() == HttpStatus.SC_OK
  }

  fun getCourses(isPublic: Boolean, currentPage: Int, enrolled: Boolean?): CoursesList? {
    val response = stepikEndpoints.courses(true, isPublic, currentPage, enrolled).executeHandlingExceptions(true)
    return response?.body()?.apply { courses.withLanguageEnvironment() }
  }

  fun getCourses(ids: Set<Int>): List<EduCourse>? {
    val response = stepikEndpoints.courses(*ids.toIntArray()).executeHandlingExceptions()
    return response?.body()?.courses?.withLanguageEnvironment()
  }

  @JvmOverloads
  fun getCourseInfo(courseId: Int, isIdeaCompatible: Boolean? = null, optional: Boolean = false): EduCourse? {
    val response = stepikEndpoints.courses(courseId, isIdeaCompatible).executeHandlingExceptions(optional)
    return response?.body()?.courses?.withLanguageEnvironment()?.firstOrNull()
  }

  // TODO: move this logic into custom deserializer
  private fun List<EduCourse>.withLanguageEnvironment(): List<EduCourse> {
    for (course in this) {
      setCourseLanguageEnvironment(course)
    }
    return this
  }

  fun getSection(sectionId: Int): Section? {
    val response = stepikEndpoints.sections(sectionId).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  fun getLesson(lessonId: Int): Lesson? {
    val response = stepikEndpoints.lessons(lessonId).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  fun getUnit(unitId: Int): StepikUnit? {
    val response = stepikEndpoints.units(unitId).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  fun getLessonUnit(lessonId: Int): StepikUnit? {
    val response = stepikEndpoints.lessonUnit(lessonId).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  fun getStep(stepId: Int): StepSource? {
    val response = stepikEndpoints.steps(stepId).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  fun getChoiceStepSource(stepId: Int): ChoiceStep? {
    val stepSource = stepikEndpoints.choiceStepSource(stepId).executeHandlingExceptions(true)?.body()?.steps?.firstOrNull()
    return stepSource?.block
  }

  fun getSubmissions(stepId: Int): List<Submission> {
    if (!isUnitTestMode && !EduSettings.isLoggedIn()) return emptyList()
    var currentPage = 1
    val allSubmissions = mutableListOf<Submission>()
    do {
      val submissionsList = stepikEndpoints.submissions(stepId, currentPage).executeHandlingExceptions()?.body() ?: break
      val submissions = submissionsList.submissions
      allSubmissions.addAll(submissions)
      currentPage += 1
    }
    while (submissions.isNotEmpty() && submissionsList.meta.containsKey("has_next") && submissionsList.meta["has_next"] == true)
    return allSubmissions
  }

  override fun getSubmission(id: Int): Result<Submission, String> =
    stepikEndpoints.submissionById(id).executeAndExtractFirst(SubmissionsList::submissions)

  override fun getActiveAttempt(task: Task): Result<Attempt?, String> {
    val userId = account?.id ?: return Err("Attempt to get list of attempts for unauthorized user")
    val attempts = stepikEndpoints.attempts(task.id, userId)
      .executeParsingErrors(true)
      .flatMap {
        val result = it.body()?.attempts
        if (result == null) Err(it.message()) else Ok(result)
      }
      .onError { return Err(it) }
    val activeAttempt = attempts.firstOrNull { it.isActive }
    return Ok(activeAttempt)
  }

  override fun getDataset(attempt: Attempt): Result<String, String> {
    return stepikEndpoints.dataset(attempt.id).executeParsingErrors().flatMap {
      val responseBody = it.body() ?: return@flatMap Err(EduCoreBundle.message("error.failed.to.parse.response"))
      Ok(responseBody.string())
    }
  }

  fun getAttempts(stepId: Int, userId: Int): List<Attempt>? {
    val response = stepikEndpoints.attempts(stepId, userId).executeHandlingExceptions()
    return response?.body()?.attempts
  }

  fun getCourseReviewSummaries(ids: List<Int>): List<CourseReviewSummary> {
    val courseIdsChunks = ids.distinct().chunked(MAX_REQUEST_PARAMS)
    val allReviewSummaries = mutableListOf<CourseReviewSummary>()
    courseIdsChunks
      .mapNotNull {
        val response = stepikEndpoints.courseReviewSummaries(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.courseReviewSummaries
      }
      .forEach { allReviewSummaries.addAll(it) }
    return allReviewSummaries
  }

  // Post requests:

  fun postCourse(course: EduCourse): EduCourse? {
    val response = stepikEndpoints.course(CourseData(course)).executeHandlingExceptions()
    return response?.body()?.courses?.firstOrNull()
  }

  fun postSection(section: Section): Section? {
    val response = stepikEndpoints.section(SectionData(section)).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  fun postLesson(lesson: Lesson): Lesson? {
    val response = stepikEndpoints.lesson(LessonData(lesson)).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  fun postUnit(lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = stepikEndpoints.unit(UnitData(lessonId, position, sectionId)).executeHandlingExceptions()
    return response?.body()?.units?.firstOrNull()
  }

  fun postTask(project: Project, task: Task, lessonId: Int): StepSource? {
    var stepSourceData: StepSourceData? = null
    try {
      invokeAndWaitIfNeeded {
        FileDocumentManager.getInstance().saveAllDocuments()
        stepSourceData = StepSourceData(project, task, lessonId)
      }
    }
    catch (e: RuntimeException) {
      val cause = e.cause as? BrokenPlaceholderException
      LOG.info("${e.message}\n${cause?.placeholderInfo}")
      return null
    }
    val response = stepikEndpoints.stepSource(stepSourceData!!).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  override fun postSubmission(submission: Submission): Result<Submission, String> {
    val submissionData = SubmissionData(submission)
    val response = stepikEndpoints.submission(submissionData).executeHandlingExceptions()
    val submissions = response?.body()?.submissions
    if (submissions.isNullOrEmpty() || response.code() != HttpStatus.SC_CREATED) {
      return Err("Failed to make submission $submissions")
    }
    if (submissions.size > 1) {
      LOG.warn("Got a submission wrapper with incorrect submissions number: ${submissions.size}")
    }
    return Ok(submissions.first())
  }

  override fun postAttempt(task: Task): Result<Attempt, String> {
    val stepId = task.id
    val response = stepikEndpoints.attempt(AttemptData(stepId)).executeHandlingExceptions(true)
    val attempt = response?.body()?.attempts?.firstOrNull()
    if (response?.code() != HttpStatus.SC_CREATED || attempt == null) {
      return Err("Failed to make attempt $stepId")
    }
    return Ok(attempt)
  }

  fun postView(assignmentId: Int, stepId: Int) {
    val response = stepikEndpoints.view(ViewData(assignmentId, stepId)).executeHandlingExceptions()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.warn("Error while Views post, code: " + response?.code())
    }
  }

  fun postMember(userId: String, group: String): Int {
    val response = stepikEndpoints.members(MemberData(userId, group)).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  fun enrollToCourse(courseId: Int) {
    val response = stepikEndpoints.enrollment(EnrollmentData(courseId)).executeHandlingExceptions()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to enroll user ${account?.id} to course $courseId")
    }
  }

  private fun postAttachment(info: AdditionalInfo, courseId: Int?, lessonId: Int?): Int {
    val fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), objectMapper.writeValueAsString(info))
    val fileData = MultipartBody.Part.createFormData("file", StepikNames.ADDITIONAL_INFO, fileBody)
    val courseBody = if (courseId != null) RequestBody.create(MediaType.parse("text/plain"), courseId.toString()) else null
    val lessonBody = if (lessonId != null) RequestBody.create(MediaType.parse("text/plain"), lessonId.toString()) else null

    val response = stepikEndpoints.attachment(fileData, courseBody, lessonBody).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  fun postCourseAttachment(info: CourseAdditionalInfo, courseId: Int) = postAttachment(info, courseId, null)

  private fun postLessonAttachment(info: LessonAdditionalInfo, lessonId: Int) = postAttachment(info, null, lessonId)

  // Update requests:

  fun updateCourse(course: Course): Int {
    val response = stepikEndpoints.course(course.id, CourseData(course)).executeHandlingExceptions()
    val postedCourse = response?.body()?.courses?.firstOrNull()
    if (postedCourse != null) {
      course.updateDate = postedCourse.updateDate
    }
    return response?.code() ?: -1
  }

  fun updateSection(section: Section): Section? {
    val response = stepikEndpoints.section(section.id, SectionData(section)).executeHandlingExceptions()
    val postedSection = response?.body()?.sections?.firstOrNull()
    if (postedSection != null) {
      section.updateDate = postedSection.updateDate
    }
    return postedSection
  }

  fun updateLesson(lesson: Lesson): Lesson? {
    val response = stepikEndpoints.lesson(lesson.id, LessonData(lesson)).executeHandlingExceptions()
    val postedLesson = response?.body()?.lessons?.firstOrNull()
    if (postedLesson != null) {
      lesson.updateDate = postedLesson.updateDate
    }
    return postedLesson
  }

  fun updateUnit(unitId: Int, lessonId: Int, position: Int, sectionId: Int): StepikUnit? {
    val response = stepikEndpoints.unit(unitId, UnitData(lessonId, position, sectionId, unitId)).executeHandlingExceptions()
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
    val response = stepikEndpoints.stepSource(task.id, stepSourceData).executeHandlingExceptions()
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
    val attachments = stepikEndpoints.attachments(courseId, lessonId).executeHandlingExceptions(true)?.body()
    if (attachments != null && attachments.attachments.isNotEmpty()) {
      val attachmentId = attachments.attachments.firstOrNull { StepikNames.ADDITIONAL_INFO == it.name }?.id
      if (attachmentId != null) {
        stepikEndpoints.deleteAttachment(attachmentId).executeHandlingExceptions()
      }
    }
  }

  // Delete requests:

  fun deleteSection(sectionId: Int) {
    stepikEndpoints.deleteSection(sectionId).executeHandlingExceptions(true)
  }

  fun deleteLesson(lessonId: Int) {
    stepikEndpoints.deleteLesson(lessonId).executeHandlingExceptions(true)
  }

  fun deleteUnit(unitId: Int) {
    stepikEndpoints.deleteUnit(unitId).executeHandlingExceptions(true)
  }

  fun deleteTask(taskId: Int) {
    stepikEndpoints.deleteStepSource(taskId).executeHandlingExceptions(true)
  }

  // Multiple requests:

  fun getUsers(result: List<EduCourse>): MutableList<StepikUserInfo> {
    val instructorIds = result.flatMap { it.instructors }.distinct().chunked(MAX_REQUEST_PARAMS)
    val allUsers = mutableListOf<StepikUserInfo>()
    instructorIds
      .mapNotNull {
        val response = stepikEndpoints.users(*it.toIntArray()).executeHandlingExceptions()
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
        val response = stepikEndpoints.sections(*it.toIntArray()).executeHandlingExceptions()
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
        val response = stepikEndpoints.lessons(*it.toIntArray()).executeHandlingExceptions()
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
        val response = stepikEndpoints.units(*it.toIntArray()).executeHandlingExceptions()
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
        val response = stepikEndpoints.assignments(*it.toIntArray()).executeHandlingExceptions()
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
        val response = stepikEndpoints.steps(*it.toIntArray()).executeHandlingExceptions()
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
        val response = stepikEndpoints.progresses(*it.toTypedArray()).executeHandlingExceptions()
        response?.body()?.progresses
      }
      .forEach { progresses.addAll(it) }

    return progresses.associate { it.id to it.isPassed }
  }

  // attachments
  open fun loadAttachment(attachmentLink: String): String? {
    try {
      val conn = URL(attachmentLink).openConnection()
      return conn.getInputStream().bufferedReader().use(BufferedReader::readText)
    }
    catch (e: IOException) {
      LOG.info("No attachments found $attachmentLink")
    }
    return null
  }

  private fun initiateAuthorizationListener(vararg postLoginActions: Runnable) =
    reconnectAndSubscribe(object : EduLogInListener {
      override fun userLoggedOut() {}

      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
      }
    })

  companion object {
    private const val MAX_REQUEST_PARAMS = 100 // restriction of Stepik API for multiple requests
    private val LOG = Logger.getInstance(StepikConnector::class.java)

    @JvmStatic
    fun getInstance(): StepikConnector = service()

    @JvmStatic
    fun createObjectMapper(module: SimpleModule): ObjectMapper {
      val objectMapper = ConnectorUtils.createMapper()
      objectMapper.addMixIn(EduCourse::class.java, StepikEduCourseMixin::class.java)
      objectMapper.addMixIn(Section::class.java, StepikSectionMixin::class.java)
      objectMapper.addMixIn(Lesson::class.java, StepikLessonMixin::class.java)
      objectMapper.addMixIn(TaskFile::class.java, StepikTaskFileMixin::class.java)
      objectMapper.addMixIn(Task::class.java, StepikTaskMixin::class.java)
      objectMapper.addMixIn(ChoiceTask::class.java, StepikChoiceTaskMixin::class.java)
      objectMapper.addMixIn(AnswerPlaceholder::class.java, StepikAnswerPlaceholderMixin::class.java)
      objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, StepikAnswerPlaceholderDependencyMixin::class.java)
      objectMapper.registerModule(module)
      return objectMapper
    }
  }
}
