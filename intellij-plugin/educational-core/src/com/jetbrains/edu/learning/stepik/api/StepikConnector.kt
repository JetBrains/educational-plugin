package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODE_ARGUMENT
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.network.toPlainTextRequestBody
import com.jetbrains.edu.learning.stepik.*
import com.jetbrains.edu.learning.stepik.StepikNames.getClientId
import com.jetbrains.edu.learning.stepik.StepikNames.getClientSecret
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.http.client.utils.URIBuilder
import java.io.BufferedReader
import java.io.IOException
import java.net.URL

abstract class StepikConnector : EduOAuthCodeFlowConnector<StepikUser, StepikUserInfo>(), StepikBasedConnector {
  override val platformName: String = StepikNames.STEPIK

  override var account: StepikUser?
    get() = EduSettings.getInstance().user
    set(account) {
      EduSettings.getInstance().user = account
    }

  override val authorizationUrlBuilder: URIBuilder
    get() {
      return URIBuilder(getStepikUrl())
        .setPath("/oauth2/authorize/")
        .addParameter("client_id", getClientId())
        .addParameter("redirect_uri", getRedirectUri())
        .addParameter("response_type", CODE_ARGUMENT)
    }

  override val clientId: String
    get() = getClientId()

  override val clientSecret: String
    get() = getClientSecret()

  override val oAuthServicePath: String
    // In case of Stepik and Android Studio our redirect_uri is `http://localhost:port`
    get() = if (EduUtilsKt.isAndroidStudio()) "" else super.oAuthServicePath

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

  // Get requests:

  override fun getUserInfo(account: StepikUser, accessToken: String?): StepikUserInfo? {
    val response = getEndpoints<StepikEndpoints>(account, accessToken).getCurrentUser().executeHandlingExceptions()
    return response?.body()?.users?.firstOrNull()
  }

  fun getLesson(lessonId: Int): StepikLesson? {
    val response = stepikEndpoints.lessons(lessonId).executeHandlingExceptions()
    return response?.body()?.lessons?.firstOrNull()
  }

  fun getStep(stepId: Int): StepSource? {
    val response = stepikEndpoints.steps(stepId).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  fun getChoiceStepSource(stepId: Int): ChoiceStep? {
    val stepSource = stepikEndpoints.choiceStepSource(stepId).executeHandlingExceptions(true)?.body()?.steps?.firstOrNull()
    return stepSource?.block
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Stepik support is dropped")
  override fun getSubmissions(stepId: Int): List<StepikBasedSubmission> = emptyList()

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Stepik support is dropped")
  override fun getSubmission(id: Int): Result<StepikBasedSubmission, String> = Err("No submission")

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Stepik support is dropped")
  override fun getActiveAttempt(task: Task): Result<Attempt?, String> = Err("No active attempt")

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Stepik support is dropped")
  override fun getDataset(attempt: Attempt): Result<String, String> = Err("No dataset")

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

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Stepik support is dropped")
  override fun postSubmission(submission: StepikBasedSubmission): Result<StepikBasedSubmission, String> = Err("Not available")

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Stepik support is dropped")
  override fun postAttempt(task: Task): Result<Attempt, String> = Err("Not available")

  private fun postLessonAttachment(info: LessonAdditionalInfo, lessonId: Int): Int {
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

    fun getInstance(): StepikConnector = service()
  }
}
