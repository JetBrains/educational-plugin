package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderDependencyMixin
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderWithAnswerMixin
import com.jetbrains.edu.learning.marketplace.GRAZIE_STAGING_URL
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.checkNotEmpty
import com.jetbrains.edu.learning.submissions.findTaskFileInDirWithSizeCheck
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection
import java.time.Instant
import java.util.*

class MarketplaceSubmissionsConnector {
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
    objectMapper
  }

  private val grazieUrl: String = GRAZIE_STAGING_URL

  init {
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private val submissionsService: SubmissionsService
    get() = submissionsService(MarketplaceSettings.INSTANCE.hubAccount?.getJwtToken())

  private fun submissionsService(accessToken: String?): SubmissionsService {
    val retrofit = createRetrofitBuilder(grazieUrl, connectionPool, accessToken = accessToken, authHeaderName = "Grazie-Authenticate-JWT", authHeaderValue = null)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(SubmissionsService::class.java)
  }

  private fun createSubmissionsDocument(
    project: Project,
    submissionDocument: SubmissionDocument,
    task: Task,
    submissionId: Int,
    showErrorNotification: Boolean = true
  ) {
    if (!isUserAuthorizedWithJwtToken()) return
    LOG.info("Creating new submission document for task ${task.name}")
    val response = submissionsService.createDocument(submissionDocument).executeParsingErrors(true)
      .flatMap {
      val resultResponse = it.body()
      return@flatMap if (resultResponse == null) Err("Nullable response body received")
      else Ok(resultResponse)
    }.onError {
      LOG.error("Failed to create submission document for task ${task.name}: ${it}")
      if (showErrorNotification) {
        CCNotificationUtils.showErrorNotification(project,
                                                  EduCoreBundle.message("notification.submissions.failed.to.post", task.name),
                                                  action = CCNotificationUtils.showLogAction)
      }
      return
    }

      val documentId = response.id
      addDocumentPath(task, documentId, submissionId)
      task.submissionsId = documentId
      LOG.info("Submission document for task ${task.name} successfully created")
  }

  private fun updateSubmissionsDocument(project: Project, submissionDocument: SubmissionDocument, task: Task) {
    if (!isUserAuthorizedWithJwtToken()) return
    LOG.info("Updating submission document with documentId = ${task.submissionsId} for task ${task.name}")
    submissionsService.updateDocument(submissionDocument).executeParsingErrors(true)
      .flatMap {
        val resultResponse = it.body()
        return@flatMap if (resultResponse == null) Err("Nullable response body received")
        else Ok(resultResponse)
      }.onError {
        LOG.error("Failed to update submission document with documentId = ${task.submissionsId} for task ${task.name}: ${it}")
        CCNotificationUtils.showErrorNotification(project,
                                                  EduCoreBundle.message("notification.submissions.failed.to.post", task.name),
                                                  action = CCNotificationUtils.showLogAction)
        return
      }
    LOG.info("Submission document with documentId = ${task.submissionsId} for task ${task.name} successfully updated")
  }

  private fun addDocumentPath(task: Task, documentId: String, submissionId: Int) {
    val path = "${task.course.id}/${task.id}/$submissionId"
    LOG.info("Adding path $path to submission document for documentId = $documentId")
    val response = submissionsService.addPathToDocument(Descriptor(documentId, path)).executeHandlingExceptions()
    if (response != null && response.isSuccessful) {
      LOG.info("Path $path for documentId = $documentId successfully added")
    }
    else {
      LOG.error("Failed to add path to submission document with documentId = $documentId")
    }
  }

  fun getAllSubmissions(course: EduCourse): MutableMap<Int, List<MarketplaceSubmission>> {
    val descriptorsList = getDescriptorsList("/${course.id}")
    val submissionsByTaskId = mutableMapOf<Int, List<MarketplaceSubmission>>()
    val documentIdByTaskId = mutableMapOf<Int, String>()
    for (descriptor in descriptorsList) {
      val taskId = parseTaskIdFromPath(descriptor.path)
      documentIdByTaskId[taskId] = descriptor.id
      submissionsByTaskId[taskId] = getSubmissions(descriptor.id)
    }

    val allTasks: List<Task> = course.allTasks
    for (task in allTasks) {
      val taskId = task.id
      if (documentIdByTaskId.containsKey(taskId)) {
        task.submissionsId = documentIdByTaskId[taskId]
      }
      // documentIdByTaskId contains already existing submissions, we should put empty lists for tasks that have no submissions yet
      else {
        submissionsByTaskId[taskId] = listOf()
      }
    }
    return submissionsByTaskId
  }

  fun getSubmissions(task: Task, courseId: Int): List<MarketplaceSubmission> {
    val documentId = getDocumentId(courseId, task.id) ?: return listOf()
    task.submissionsId = documentId
    return getSubmissions(documentId)
  }

  private fun getSubmissions(documentId: String): List<MarketplaceSubmission> {
    val versionsList = getDocVersionsIds(documentId) ?: return listOf()
    return versionsList.mapNotNull { getSubmission(documentId, it) }
  }

  @VisibleForTesting
  fun getSubmission(documentId: String, version: Version): MarketplaceSubmission? {
    val submissionDocument = SubmissionDocument(documentId, versionId = version.id)
    val response = submissionsService.getSubmissionContent(submissionDocument).executeHandlingExceptions()
    val content = response?.body()?.content
    val submissionContent = objectMapper.readValue(content, Content::class.java) ?: return null
    // TODO: double wrapping into "content" is a bug on grazie side, should be fixed on our side when ready
    val submission = objectMapper.readValue(submissionContent.content, MarketplaceSubmission::class.java) ?: return null
    submission.time = Date.from(Instant.ofEpochSecond(version.timestamp))
    return submission
  }

  @VisibleForTesting
  fun getDocVersionsIds(documentId: String): List<Version>? {
    val document = Document(documentId)
    val response = submissionsService.getVersionsList(document).executeHandlingExceptions()
    return response?.body()?.versionsList
  }

  @VisibleForTesting
  fun getDocumentId(courseId: Int, taskId: Int): String? {
    val path = "/$courseId/$taskId"
    val descriptorsList = getDescriptorsList(path)
    if (descriptorsList.isEmpty()) return null
    if (descriptorsList.size > 1) error("More than one descriptor found for task $taskId")
    val documentId = descriptorsList[0].id
    LOG.info("Successfully loaded documentId for taskId = $taskId")
    return documentId
  }

  private fun getDescriptorsList(path: String): List<Descriptor> {
    LOG.info("Loading descriptors list for path = $path")
    val response = submissionsService.getDescriptorsList(DocumentPath(path)).executeHandlingExceptions()
    if (response != null && response.isSuccessful) {
      return response.body()?.descriptorsList ?: return emptyList()
    }
    else {
      LOG.error("Failed to load descriptors list for path = $path")
      return emptyList()
    }
  }

  // Descriptor path format: /courseId/taskId/submissionId,
  // ex.: /16630/186011/2009178346
  private fun parseTaskIdFromPath(path: String): Int {
    return try {
      Integer.parseInt(path.split("/")[TASK_ID_INDEX])
    }
    catch (e: NumberFormatException) {
      throw DescriptorPathFormatException(path, EduCoreBundle.message("error.incorrect.task.id.format", e.message.toString()))
    }
    catch(e: IndexOutOfBoundsException) {
      throw DescriptorPathFormatException(path, EduCoreBundle.message("error.incorrect.descriptor.path.format", e.message.toString()))
    }
  }

  fun markTheoryTaskAsCompleted(project: Project, task: TheoryTask) {
    if (task.submissionsId == null) {
      val emptySubmissionData = MarketplaceSubmission(task)
      val submissionId = emptySubmissionData.id ?: error("Submission id not generated at creation")
      val submissionDocument = SubmissionDocument(docId = task.submissionsId,
                                                  submissionContent = ObjectMapper().writeValueAsString(emptySubmissionData).trimIndent())

      createSubmissionsDocument(project, submissionDocument, task, submissionId, false)
    }
  }

  fun isJwtTokenValid(token: String): Boolean {
    val submissionServiceWithCheckedToken = submissionsService(token)
    // any path can be used, if token is valid - empty list with HttpStatus.OK will be returned, HttpStatus.SC_UNAUTHORIZED otherwise
    val path = "/testPath"
    LOG.info("Validating inserted token: $token")
    val response = submissionServiceWithCheckedToken.getDescriptorsList(DocumentPath(path)).executeHandlingExceptions()
    return if (response != null && response.code() != HttpURLConnection.HTTP_UNAUTHORIZED) {
      LOG.info("Inserted token: $token is valid")
      true
    }
    else {
      LOG.error("Invalid token: $token inserted")
      false
    }
  }

  fun postSubmission(project: Project, task: Task): MarketplaceSubmission {
    val submission = MarketplaceSubmission(task.id, task.status, getSolutionFiles(project, task), task.course.marketplaceCourseVersion)
    val submissionDocument = SubmissionDocument(docId = task.submissionsId,
                                                submissionContent = objectMapper.writeValueAsString(submission).trimIndent())
    if (task.submissionsId == null) {
      val submissionId = submission.id ?: error("Submission id not generated at creation")
      createSubmissionsDocument(project, submissionDocument, task, submissionId)
    }
    else {
      updateSubmissionsDocument(project, submissionDocument, task)
    }
    return submission
  }

  private fun getSolutionFiles(project: Project, task: Task): List<SolutionFile> {
    val files = mutableListOf<SolutionFile>()
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find task directory ${task.name}")

    for (taskFile in task.taskFiles.values) {
      val virtualFile = findTaskFileInDirWithSizeCheck(taskFile, taskDir) ?: continue

      runReadAction {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
        files.add(SolutionFile(taskFile.name, document.text, taskFile.isVisible, taskFile.answerPlaceholders))
      }
    }

    return files.checkNotEmpty()
  }

  companion object {
    private val LOG = logger<MarketplaceConnector>()
    private const val TASK_ID_INDEX = 2

    @JvmStatic
    val GRAZIE_AUTHORIZATION_TOPIC = Topic.create("Edu.grazieLoggedIn", EduLogInListener::class.java)

    @JvmStatic
    fun getInstance(): MarketplaceSubmissionsConnector = service()

    fun isUserAuthorizedWithJwtToken(): Boolean {
      val user = MarketplaceSettings.INSTANCE.hubAccount
      if (user == null) {
        // we check that user isn't null before `postCourse` call
        LOG.warn("User is null when posting the course")
        return false
      }
      else if (!user.isJwtTokenProvided()){
          LOG.warn("User is not authorised to submissions repository when posting submissions. Jwt token is empty.")
          return false
        }
      return true
    }
  }

  private class DescriptorPathFormatException(descriptorPath: String, details: String) : IllegalStateException(
    buildString {
      appendLine(EduCoreBundle.message("error.incorrect.descriptor.path.message", descriptorPath))
      appendLine(details)
    })
}