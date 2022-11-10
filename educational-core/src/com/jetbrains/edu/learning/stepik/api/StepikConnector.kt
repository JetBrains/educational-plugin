package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.authUtils.OAuthRestService.CODE_ARGUMENT
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.*
import com.jetbrains.edu.learning.stepik.StepikNames.getClientId
import com.jetbrains.edu.learning.stepik.StepikNames.getClientSecret
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl
import com.jetbrains.edu.learning.stepik.course.StepikLesson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.annotations.NonNls
import java.io.BufferedReader
import java.io.IOException
import java.net.URL

abstract class StepikConnector : EduOAuthConnector<StepikUser, StepikUserInfo>(), StepikBasedConnector {
  override val platformName: String = StepikNames.STEPIK

  override var account: StepikUser?
    get() = EduSettings.getInstance().user
    set(account) {
      EduSettings.getInstance().user = account
    }

  override val authorizationUrl: String
    get() = URIBuilder(getStepikUrl())
      .setPath("/oauth2/authorize/")
      .addParameter("client_id", getClientId())
      .addParameter("redirect_uri", getRedirectUri())
      .addParameter("response_type", CODE_ARGUMENT)
      .build()
      .toString()

  override val clientId: String
    get() = getClientId()

  override val clientSecret: String
    get() = getClientSecret()

  override val oAuthServicePath: String
    // In case of Stepik and Android Studio our redirect_uri is `http://localhost:port`
    get() = if (EduUtils.isAndroidStudio()) "" else super.oAuthServicePath

  override val objectMapper: ObjectMapper by lazy {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    module.addDeserializer(Reply::class.java, StepikReplyDeserializer())
    StepikBasedConnector.createObjectMapper(module)
  }

  private val stepikEndpoints: StepikEndpoints
    get() = getEndpoints()

  override fun doRefreshTokens() {
    refreshTokens()
  }

  // Authorization requests:

  override fun doAuthorize(
    vararg postLoginActions: Runnable,
    authorizationPlace: EduCounterUsageCollector.AuthorizationPlace) {
    super.doAuthorize(*postLoginActions, authorizationPlace = authorizationPlace)

    // EDU-4767
    val redirectUrl = getRedirectUri()
    if (redirectUrl == EXTERNAL_REDIRECT_URL) {
      val dialog = OAuthDialog()
      dialog.show()
    }
  }

  @Synchronized
  override fun login(code: String): Boolean {
    val tokenInfo = retrieveLoginToken(code, getRedirectUri()) ?: return false
    val stepikUser = StepikUser(tokenInfo)
    val currentUser = getUserInfo(stepikUser, tokenInfo.accessToken) ?: return false
    if (currentUser.isGuest) {
      // it means that session is broken, so we should force user to re-login
      LOG.warn("User ${currentUser.getFullName()} is anonymous")
      account = null
      return false
    }
    stepikUser.userInfo = currentUser
    stepikUser.saveTokens(tokenInfo)
    account = stepikUser
    return true
  }

  override fun getRedirectUri(): String {
    return try {
      super.getRedirectUri()
    } catch (e: IOException) {
      EXTERNAL_REDIRECT_URL
    }
  }

  // Get requests:

  override fun getUserInfo(account: StepikUser, accessToken: String?): StepikUserInfo? {
    val response = getEndpoints<StepikEndpoints>(account, accessToken).getCurrentUser().executeHandlingExceptions()
    return response?.body()?.users?.firstOrNull()
  }

  fun isEnrolledToCourse(courseId: Int): Boolean {
    val response = stepikEndpoints.enrollments(courseId).executeHandlingExceptions(true)
    return response?.code() == HttpStatus.SC_OK
  }

  fun getCourses(isPublic: Boolean, currentPage: Int, enrolled: Boolean?): CoursesList? {
    val response = stepikEndpoints.courses(true, isPublic, currentPage, enrolled).executeHandlingExceptions(true)
    return response?.body()
  }

  fun getCourses(ids: Set<Int>): List<EduCourse>? {
    val response = stepikEndpoints.courses(*ids.toIntArray()).executeHandlingExceptions()
    return response?.body()?.courses
  }

  @JvmOverloads
  fun getCourseInfo(courseId: Int, isIdeaCompatible: Boolean? = null, optional: Boolean = false): EduCourse? {
    val response = stepikEndpoints.courses(courseId, isIdeaCompatible).executeHandlingExceptions(optional)
    return response?.body()?.courses?.firstOrNull()
  }

  fun getSection(sectionId: Int): StepikSection? {
    val response = stepikEndpoints.sections(sectionId).executeHandlingExceptions()
    return response?.body()?.sections?.firstOrNull()
  }

  fun getLesson(lessonId: Int): StepikLesson? {
    val response = stepikEndpoints.lessons(lessonId).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
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

  fun getSubmissions(stepId: Int): List<StepikBasedSubmission> {
    if (!isUnitTestMode && !EduSettings.isLoggedIn()) return emptyList()
    var currentPage = 1
    val allSubmissions = mutableListOf<StepikBasedSubmission>()
    do {
      val submissionsList = stepikEndpoints.submissions(stepId, currentPage).executeHandlingExceptions()?.body() ?: break
      val submissions = submissionsList.submissions
      allSubmissions.addAll(submissions)
      currentPage += 1
    }
    while (submissions.isNotEmpty() && submissionsList.meta.containsKey("has_next") && submissionsList.meta["has_next"] == true)
    return allSubmissions
  }

  override fun getSubmission(id: Int): Result<StepikBasedSubmission, String> {
    return withTokenRefreshIfFailed {
      val submission = stepikEndpoints.submissionById(id).executeHandlingExceptions()?.body()?.submissions?.firstOrNull()
                       ?: return@withTokenRefreshIfFailed Err("Failed to load submission with id = $id")
      Ok(submission)
    }
  }

  override fun getActiveAttempt(task: Task): Result<Attempt?, String> {
    return withTokenRefreshIfFailed {
      val userId = account?.id ?: return@withTokenRefreshIfFailed Err("Attempt to get list of attempts for unauthorized user")
      val attempts = stepikEndpoints.attempts(task.id, userId)
        .executeParsingErrors(true)
        .flatMap {
          val result = it.body()?.attempts
          if (result == null) Err(it.message()) else Ok(result)
        }
        .onError { return@withTokenRefreshIfFailed Err(it) }
      val activeAttempt = attempts.firstOrNull { it.isActive }
      Ok(activeAttempt)
    }
  }

  override fun getDataset(attempt: Attempt): Result<String, String> {
    return stepikEndpoints.dataset(attempt.id).executeParsingErrors().flatMap {
      val responseBody = it.body() ?: return@flatMap Err(EduCoreBundle.message("error.failed.to.parse.response"))
      Ok(responseBody.string())
    }
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

  fun postLesson(lesson: Lesson): StepikLesson? {
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

  override fun postSubmission(submission: StepikBasedSubmission): Result<StepikBasedSubmission, String> {
    val submissionData = SubmissionData(submission)
    return withTokenRefreshIfFailed {
      val response = stepikEndpoints.submission(submissionData).executeHandlingExceptions()
      val submissions = response?.body()?.submissions
      if (submissions.isNullOrEmpty() || response.code() != HttpStatus.SC_CREATED) {
        return@withTokenRefreshIfFailed Err("Failed to make submission $submissions")
      }
      if (submissions.size > 1) {
        LOG.warn("Got a submission wrapper with incorrect submissions number: ${submissions.size}")
      }
      Ok(submissions.first())
    }
  }

  override fun postAttempt(task: Task): Result<Attempt, String> {
    val stepId = task.id
    return withTokenRefreshIfFailed {
      val response = stepikEndpoints.attempt(AttemptData(stepId)).executeParsingErrors().onError { return@withTokenRefreshIfFailed Err(it) }
      val attempt = response.body()?.attempts?.firstOrNull() ?:  return@withTokenRefreshIfFailed Err("Failed to make attempt $stepId")
      Ok(attempt)
    }
  }

  fun postView(assignmentId: Int, stepId: Int) {
    withTokenRefreshIfFailed {
      val response = stepikEndpoints.view(ViewData(assignmentId, stepId)).executeHandlingExceptions()
      if (response?.code() != HttpStatus.SC_CREATED) {
        return@withTokenRefreshIfFailed Err("Error while Views post, code: " + response?.code())
      }
      Ok(response)
    }
  }

  fun enrollToCourse(courseId: Int) {
    val response = stepikEndpoints.enrollment(EnrollmentData(courseId)).executeHandlingExceptions()
    if (response?.code() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to enroll user ${account?.id} to course $courseId")
    }
  }

  private fun postLessonAttachment(info: LessonAdditionalInfo, lessonId: Int) : Int {
    val fileBody = objectMapper.writeValueAsString(info).toRequestBody("multipart/form-data".toMediaTypeOrNull())
    val fileData = MultipartBody.Part.createFormData("file", StepikNames.ADDITIONAL_INFO, fileBody)
    val lessonBody = lessonId.toString().toPlainTextRequestBody()

    val response = stepikEndpoints.attachment(fileData, lessonBody).executeHandlingExceptions()
    return response?.code() ?: -1
  }

  // Update requests:

  fun updateLesson(lesson: Lesson): StepikLesson? {
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

  fun updateLessonAttachment(info: LessonAdditionalInfo, lesson: Lesson): Int {
    deleteLessonAttachment(lesson.id)
    updateLesson(lesson) // Needed to push forward update_date in lesson
    return postLessonAttachment(info, lesson.id)
  }

  fun deleteLessonAttachment(lessonId: Int) {
    val attachments = stepikEndpoints.attachments(lessonId).executeHandlingExceptions(true)?.body()
    if (attachments != null && attachments.attachments.isNotEmpty()) {
      val attachmentId = attachments.attachments.firstOrNull { StepikNames.ADDITIONAL_INFO == it.name }?.id
      if (attachmentId != null) {
        stepikEndpoints.deleteAttachment(attachmentId).executeHandlingExceptions()
      }
    }
  }

  // Delete requests:

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

  fun getSections(sectionIds: List<Int>): List<StepikSection> {
    val sectionIdsChunks = sectionIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allSections = mutableListOf<StepikSection>()
    sectionIdsChunks
      .mapNotNull {
        val response = stepikEndpoints.sections(*it.toIntArray()).executeHandlingExceptions()
        response?.body()?.sections
      }
      .forEach { allSections.addAll(it) }
    return allSections
  }

  fun getLessons(lessonIds: List<Int>): List<StepikLesson> {
    val lessonsIdsChunks = lessonIds.distinct().chunked(MAX_REQUEST_PARAMS)
    val allLessons = mutableListOf<StepikLesson>()
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

  companion object {
    private const val MAX_REQUEST_PARAMS = 100 // restriction of Stepik API for multiple requests
    private val LOG = Logger.getInstance(StepikConnector::class.java)

    @NonNls
    private const val EXTERNAL_REDIRECT_URL: String = "https://example.com"

    @JvmStatic
    fun getInstance(): StepikConnector = service()
  }
}
