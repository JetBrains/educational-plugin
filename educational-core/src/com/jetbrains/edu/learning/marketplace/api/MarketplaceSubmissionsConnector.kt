package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.PermanentInstallationID
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBAccountInfoService
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderDependencyMixin
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderWithAnswerMixin
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedToDeleteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showNoSubmissionsToDeleteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showSubmissionsDeletedSucessfullyNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionSharingPreference
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.checkNotEmpty
import com.jetbrains.edu.learning.submissions.findTaskFileInDirWithSizeCheck
import okhttp3.ConnectionPool
import okhttp3.ResponseBody
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.BufferedInputStream
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.URL

@Service(Service.Level.APP)
class MarketplaceSubmissionsConnector {
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(SolutionFile::class.java, SolutionFileMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
    objectMapper
  }

  private val submissionsServiceUrl: String
    get() = SubmissionsServiceHost.getSelectedUrl()

  init {
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private val submissionsService: SubmissionsService
    get() = submissionsService()

  private fun submissionsService(): SubmissionsService {
    val uidToken = if (RemoteEnvHelper.isRemoteDevServer()) {
      RemoteEnvHelper.getUserUidToken() ?: error("User UID was not found, it might require more time to retrieve it")
    }
    else {
      JBAccountInfoService.getInstance()?.userData?.id
    }

    if (uidToken == null) {
      LOG.warn("Nullable JB account ID token in user data")
      return submissionsServiceWithAnonymousUser()
    }

    val retrofit = createRetrofitBuilder(submissionsServiceUrl, connectionPool, "u.$uidToken")
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(SubmissionsService::class.java)
  }

  private fun submissionsServiceWithAnonymousUser(): SubmissionsService {
    val installationId = PermanentInstallationID.get()
    val retrofit = createRetrofitBuilder(
      baseUrl = submissionsServiceUrl,
      connectionPool = connectionPool,
      accessToken = installationId,
      authHeaderName = ANONYMOUS_AUTHORIZATION_HEADER,
      authHeaderValue = null
    )
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(SubmissionsService::class.java)
  }

  fun deleteAllSubmissions(project: Project?, loginName: String?): Boolean {
    LOG.info("Deleting submissions${loginName.toLogString()}")

    val response = submissionsService.deleteAllSubmissions().executeCall().onError {
      LOG.error("Failed to delete all submissions${loginName.toLogString()}. Error message: $it")
      showFailedToDeleteNotification(project, loginName)
      return false
    }
    logAndNotifyAfterDeletionAttempt(response, project, loginName)

    return response.code() == HTTP_NO_CONTENT
  }

  private fun String?.toLogString(): String = if (this != null) " for user $this" else ""

  fun getAllSubmissions(courseId: Int): List<MarketplaceSubmission> {
    var currentPage = 1
    val allSubmissions = mutableListOf<MarketplaceSubmission>()
    do {
      val submissionsList = submissionsService.getAllSubmissionsForCourse(courseId, currentPage).executeHandlingExceptions()?.body()
                            ?: break
      val submissions = submissionsList.submissions
      allSubmissions.addAll(submissions)
      currentPage += 1
    }
    while (submissions.isNotEmpty() && submissionsList.hasNext)
    return allSubmissions
  }

  /**
   * Fetches only N shared solutions for each task on the course that user has solved
   */
  fun getSharedSolutionsForCourse(courseId: Int, updateVersion: Int): List<MarketplaceSubmission> {
    LOG.info("Loading all published solutions for courseId = $courseId")

    return generateSequence(1) { it + 1 }
      .mapNotNull { fetchSharedSolutionsForCourse(courseId, updateVersion, it) }
      .takeWhile { it.hasNext && it.submissions.isNotEmpty() }
      .flatMap { it.submissions }
      .toList()
  }

  fun markTheoryTaskAsCompleted(task: TheoryTask) {
    val emptySubmission = MarketplaceSubmission(
      task.id,
      CheckStatus.Solved,
      "",
      null,
      task.course.marketplaceCourseVersion,
      PermanentInstallationID.get()
    )
    LOG.info("Marking theory task ${task.name} as completed")
    doPostSubmission(task.course.id, task.id, emptySubmission)
  }

  @RequiresBackgroundThread
  fun loadSolutionFiles(solutionKey: String): List<SolutionFile> {

    val solutionsDownloadLink = submissionsService.getSolutionDownloadLink(solutionKey).executeParsingErrors().onError {
      error("failed to obtain download link for solution key $solutionKey")
    }.body()?.string() ?: error("Nullable solutionsDownloadLink")

    val solutions: String = loadSolutionByLink(solutionsDownloadLink)

    return objectMapper.readValue(solutions, object : TypeReference<List<SolutionFile>>() {})
           ?: error("Failed to load solution files for solution key $solutionKey")
  }

  fun postSubmission(project: Project, task: Task): MarketplaceSubmission {
    val solutionFiles = solutionFilesList(project, task).filter { it.isVisible }
    val solutionText = objectMapper.writeValueAsString(solutionFiles).trimIndent()

    val course = task.course

    val submission = MarketplaceSubmission(
      task.id,
      task.status,
      solutionText,
      solutionFiles,
      course.marketplaceCourseVersion,
      PermanentInstallationID.get()
    )

    val postedSubmission = doPostSubmission(course.id, task.id, submission).onError { error("failed to post submission") }
    submission.id = postedSubmission.id
    submission.time = postedSubmission.time
    return submission
  }

  suspend fun changeSharingPreference(state: Boolean): Result<Response<Unit>, String> {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Changing solution sharing to state $state for user $loginName")
    val newSharingPreference = if (state) MarketplaceSolutionSharingPreference.ALWAYS else MarketplaceSolutionSharingPreference.NEVER
    return try {
      Ok(submissionsService.changeSharingPreference(newSharingPreference.name))
    }
    catch (e: Exception) {
      LOG.info("Error occurred while changing solution sharing to state $state for user $loginName", e)
      Err(e.message ?: "Something went wrong")
    }
  }

  suspend fun getSharingPreference(): MarketplaceSolutionSharingPreference? {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Getting solution sharing preference for use $loginName")
    val responseString = try {
      submissionsService.getSharingPreference().string()
    }
    catch (e: Exception) {
      LOG.info("Error occurred while getting solution sharing preference for user $loginName", e)
      null
    }

    return responseString?.let { MarketplaceSolutionSharingPreference.valueOf(it) }
  }

  private fun fetchSharedSolutionsForCourse(
    courseId: Int,
    updateVersion: Int,
    page: Int
  ): MarketplaceSubmissionsList? = submissionsService.getAllPublicSubmissionsForCourse(
    courseId,
    updateVersion,
    page
  ).executeHandlingExceptions()?.body()

  private fun doPostSubmission(courseId: Int, taskId: Int, submission: MarketplaceSubmission): Result<MarketplaceSubmission, String> {
    LOG.info("Posting submission for task $taskId")
    return submissionsService.postSubmission(courseId, submission.courseVersion, taskId, submission).executeParsingErrors().flatMap {
      val result = it.body()
      if (result == null) Err(EduCoreBundle.message("error.failed.to.post.solution")) else Ok(result)
    }
  }

  private fun solutionFilesList(project: Project, task: Task): List<SolutionFile> {
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

  private fun logAndNotifyAfterDeletionAttempt(response: Response<ResponseBody>, project: Project?, loginName: String?) {
    when (response.code()) {
      HTTP_NO_CONTENT -> {
        LOG.info("Successfully deleted all submissions${loginName.toLogString()}")
        showSubmissionsDeletedSucessfullyNotification(project, loginName)
      }

      HTTP_NOT_FOUND -> {
        LOG.info("There are no submissions to delete${loginName.toLogString()}")
        showNoSubmissionsToDeleteNotification(project, loginName)
      }

      else -> {
        val errorMsg = response.errorBody()?.string() ?: "Unknown error"
        LOG.error("Failed to delete all submissions for user $loginName. Error message: $errorMsg")
        showFailedToDeleteNotification(project, loginName)
      }
    }
  }

  companion object {
    @NonNls
    private const val ANONYMOUS_AUTHORIZATION_HEADER: String = "User-PID"

    private val LOG = logger<MarketplaceConnector>()

    @VisibleForTesting
    fun loadSolutionByLink(solutionsDownloadLink: String): String {
      return URL(solutionsDownloadLink).openConnection().getInputStream().use {
        val inputStream = BufferedInputStream(it)
        String(inputStream.readAllBytes())
      }
    }

    fun getInstance(): MarketplaceSubmissionsConnector = service()
  }
}