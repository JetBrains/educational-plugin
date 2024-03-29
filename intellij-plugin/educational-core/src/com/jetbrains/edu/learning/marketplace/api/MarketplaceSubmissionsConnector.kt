package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
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
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderDependencyMixin
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderWithAnswerMixin
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedToDeleteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showNoSubmissionsToDeleteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showSubmissionsDeletedSucessfullyNotification
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.marketplace.userAgreement.UserAgreementDialogResultState
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeCall
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.network.executeParsingErrors
import com.jetbrains.edu.learning.submissions.*
import okhttp3.ConnectionPool
import okhttp3.ResponseBody
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
    val uidToken = if (RemoteEnvHelper.isRemoteDevServer() && !isUnitTestMode) {
      RemoteEnvHelper.getUserUidToken() ?: error("User UID was not found, it might require more time to retrieve it")
    }
    else {
      JBAccountInfoService.getInstance()?.userData?.id ?: error("Nullable JB account ID token in user data")
    }

    val retrofit = createRetrofitBuilder(submissionsServiceUrl, connectionPool, "u.$uidToken")
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
    val userAgreementState = getUserAgreementState()
    if (!userAgreementState.isSubmissionDownloadAllowed()) {
      LOG.info("Submissions will not be loaded because User Agreement state is $userAgreementState")
      return emptyList()
    }
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
    LOG.info("Loading all published solutions for course with courseId = $courseId, updateVersion = $updateVersion")

    val courseSharedSolutions = mutableListOf<MarketplaceSubmission>()
    var currentPage = 1
    do {
      val submissionsList = fetchSharedSolutionsForCourse(courseId, updateVersion, currentPage)
                            ?: break
      val sharedSolutions = submissionsList.submissions
      courseSharedSolutions.addAll(sharedSolutions)
      currentPage += 1
    }
    while (sharedSolutions.isNotEmpty() && submissionsList.hasNext)

    return courseSharedSolutions
  }

  fun getSharedSolutionsForTask(course: Course, taskId: Int): List<MarketplaceSubmission> {
    val (courseId, updateVersion) = course.id to course.marketplaceCourseVersion
    LOG.info("Loading shared solutions for task $taskId on course with courseId = $courseId, updateVersion = $updateVersion")

    val courseSharedSolutions = mutableListOf<MarketplaceSubmission>()
    var currentPage = 1
    do {
      val submissionsList = submissionsService.getPublicSubmissionsForTask(
        courseId, updateVersion, taskId, currentPage
      ).executeHandlingExceptions()?.body() ?: break

      val sharedSolutions = submissionsList.submissions
      courseSharedSolutions.addAll(sharedSolutions)
      currentPage += 1
    }
    while (sharedSolutions.isNotEmpty() && submissionsList.hasNext)

    return courseSharedSolutions
  }

  fun markTheoryTaskAsCompleted(task: TheoryTask) {
    val emptySubmission = MarketplaceSubmission(
      task.id,
      CheckStatus.Solved,
      "",
      null,
      task.course.marketplaceCourseVersion
    )
    LOG.info("Marking theory task ${task.name} as completed")
    doPostSubmission(task.course.id, task.id, emptySubmission)
  }

  @RequiresBackgroundThread
  fun loadSolutionFiles(solutionKey: String): List<SolutionFile> {

    val solutionsDownloadLink = submissionsService.getSolutionDownloadLink(solutionKey).executeParsingErrors().onError {
      LOG.warn("Failed to obtain a download link for solution key $solutionKey: $it")
      return emptyList()
    }.body()?.string() ?: error("Nullable solutionsDownloadLink")

    val solutions: String = loadSolutionByLink(solutionsDownloadLink)

    return objectMapper.readValue(solutions, object : TypeReference<List<SolutionFile>>() {})
           ?: error("Failed to load solution files for solution key $solutionKey")
  }

  fun postSubmission(project: Project, task: Task, result: CheckResult): MarketplaceSubmission {
    val solutionFiles = solutionFilesList(project, task).filter { it.isVisible }
    val solutionText = objectMapper.writeValueAsString(solutionFiles).trimIndent()

    val course = task.course

    val submission = MarketplaceSubmission(
      task.id,
      task.status,
      solutionText,
      solutionFiles,
      course.marketplaceCourseVersion,
      result.executedTestsInfo
    )

    val postedSubmission = doPostSubmission(course.id, task.id, submission).onError { error("failed to post submission") }
    submission.id = postedSubmission.id
    submission.time = postedSubmission.time
    return submission
  }

  suspend fun changeSharingPreference(state: Boolean): Result<Response<Unit>, String> {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Changing solution sharing to state $state for user $loginName")
    val newSharingPreference = if (state) SolutionSharingPreference.ALWAYS else SolutionSharingPreference.NEVER
    return try {
      Ok(submissionsService.changeSharingPreference(newSharingPreference.name))
    }
    catch (e: Exception) {
      LOG.info("Error occurred while changing solution sharing to state $state for user $loginName", e)
      Err(e.message ?: "Something went wrong")
    }
  }

  suspend fun getSharingPreference(): SolutionSharingPreference? {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Getting solution sharing preference for use $loginName")
    val responseString = try {
      submissionsService.getSharingPreference().string()
    }
    catch (e: Exception) {
      LOG.info("Error occurred while getting solution sharing preference for user $loginName", e)
      null
    }

    return responseString?.let { SolutionSharingPreference.valueOf(it) }
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

  fun reportSolution(submissionId: Int): Boolean {
    LOG.info("Reporting solution with id $submissionId")
    submissionsService.reportSolution(submissionId).executeParsingErrors().onError {
      LOG.info("Failed to report solution with id $submissionId")
      return false
    }

    return true
  }

  private fun doPostSubmission(courseId: Int, taskId: Int, submission: MarketplaceSubmission): Result<MarketplaceSubmission, String> {
    val userAgreementState = getUserAgreementState()
    if (!userAgreementState.isSubmissionUploadAllowed()) {
      LOG.info("User Agreement not accepted, submission for task $taskId will not be posted")
      return Err("User Agreement not accepted")
    }
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

  @RequiresBackgroundThread
  fun getUserAgreementState(): UserAgreementState? {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName ?: run {
      LOG.info("Unable to get user agreement state for not logged in user")
      return null
    }
    LOG.info("Getting user agreement state for user $loginName")
    val response = submissionsService.getUserAgreementState().executeParsingErrors().onError {
      LOG.info("Error occurred while getting user agreement state for user $loginName")
      return null
    }

    return response.body()?.string()?.let { UserAgreementState.valueOf(it) }
  }

  suspend fun changeUserAgreementAndStatisticsState(result: UserAgreementDialogResultState) {
    changeUserAgreementState(result.agreementState)
    changeUserStatisticsAllowedState(result.isStatisticsSharingAllowed)
  }

  suspend fun changeUserAgreementState(newState: UserAgreementState): Result<Unit, String> {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    val newStateName = newState.name
    LOG.info("Changing User Agreement state to $newStateName for user $loginName")
    return try {
      val response = submissionsService.changeUserAgreementState(newStateName)
      if (response.isSuccessful) {
        Ok(Unit)
      }
      else {
        Err("Failed to change User Agreement state: ${response.errorBody()}. Response code: ${response.code()}")
      }
    }
    catch (e: Exception) {
      LOG.error("Error occurred while changing User Agreement state to $newStateName for user $loginName", e)
      Err(e.message ?: "Failed to update User Agreement state")
    }
  }

  suspend fun changeUserStatisticsAllowedState(newState: Boolean): Result<Unit, String> {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Changing User Statistics Allowed state to $newState for user $loginName")
    return try {
      val response = submissionsService.changeUserStatisticsAllowedState(newState)
      if (response.isSuccessful) {
        Ok(Unit)
      }
      else {
        Err("Failed to change User Statistics Allowance state: ${response.errorBody()}. Response code: ${response.code()}")
      }
    }
    catch (e: Exception) {
      LOG.error("Error occurred while changing User Statistics Allowed state to $newState for user $loginName", e)
      Err(e.message ?: "Failed to change User Statistics Allowed state")
    }
  }

  suspend fun getUserStatisticsAllowedState(): Boolean? {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    val response = submissionsService.getUserStatisticsAllowedState()
    val responseBody = response.body()
    if (responseBody == null) {
      LOG.info("Error occurred while getting User Agreement state for user $loginName: ${response.errorBody()}. Response code: ${response.code()}")
    }
    return responseBody
  }

  companion object {
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