package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getLanguages
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesAccount
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesUserInfo
import com.jetbrains.edu.learning.courseFormat.codeforces.ContestParameters
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.network.checkStatusCode
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.network.executeParsingErrors
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.stepik.api.EduTaskReply
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class CodeforcesConnector {
  @VisibleForTesting
  val objectMapper: ObjectMapper
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  private val TIMEOUT_IN_SEC = TimeUnit.MINUTES.toSeconds(10)
  private val handleRegex = """var handle = "([\w\-]+)"""".toRegex()

  init {
    val module = SimpleModule()
    objectMapper = ConnectorUtils.createRegisteredMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  protected abstract val baseUrl: String

  protected val serviceHolder: SynchronizedClearableLazy<CodeforcesService> = SynchronizedClearableLazy { service() }

  private val service: CodeforcesService
    get() = serviceHolder.value

  private fun service(): CodeforcesService =
    createRetrofitBuilder(baseUrl, connectionPool, customInterceptor = AntiCrawlerInterceptor)
      .addConverterFactory(converterFactory)
      .build()
      .create(CodeforcesService::class.java)

  private object AntiCrawlerInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
      val response = chain.proceed(chain.request())
      if (response.isSuccessful) {
        // original redirect page length is about 900 characters, so 1024 is enough to get it
        // and cut real contest data when anti-cowling disabled
        val text = response.peekBody(1024).string()
        if (text.contains("Redirecting... Please, wait.")) {
          response.close()
          throw CoursesDownloadingException("${EduCoreBundle.message("codeforces.anti.crawler.start")} ${EduCoreBundle.message("codeforces.anti.crawler.end1")}")
        }
      }
      return response
    }
  }


  fun getContests(withTrainings: Boolean = false, locale: String = "en"): ContestsResponse? =
    service.contests(withTrainings, locale)
      .executeHandlingExceptions()
      ?.checkStatusCode()
      ?.body()

  fun getContest(contestParameters: ContestParameters): Result<CodeforcesCourse, String> =
    handlingDownloadException {
      service.problems(contestParameters.id, contestParameters.locale).executeParsingErrors().flatMap {
        val responseBody = it.body() ?: return@flatMap Err(EduCoreBundle.message("error.failed.to.parse.response"))
        val doc = Jsoup.parse(responseBody.string())
        val codeforcesCourse = CodeforcesCourse(contestParameters)
        codeforcesCourse.parseResponseToAddContent(doc)
        Ok(codeforcesCourse)
      }
    }

  fun getContestInformation(contestId: Int): Result<CodeforcesCourse, String> =
    handlingDownloadException {
      val contestsList = getContests() ?: return Err(EduCoreBundle.message("codeforces.error.failed.to.get.contests.list"))
      val contestInfo = contestsList.result.find { it.id == contestId }
                        ?: return Err(EduCoreBundle.message("codeforces.error.failed.to.find.contest.in.contests.list"))

      val responseBody = service.status(contestId).executeParsingErrors()
                           .onError { return Err(it) }
                           .body() ?: return Err(EduCoreBundle.message("error.failed.to.parse.response"))
      val doc = Jsoup.parse(responseBody.string())
      val contestLanguages = getLanguages(doc) ?: return Err(EduCoreBundle.message("codeforces.error.failed.to.get.contest.language"))

      val contestParameters = ContestParameters(
        contestId, name = contestInfo.name, endDateTime = contestInfo.endTime,
        availableLanguages = contestLanguages
      )
      return Ok(CodeforcesCourse(contestParameters))
    }

  fun getContestsPage(): Result<Document, String> {
    return service.contestsPage().executeParsingErrors().flatMap {
      val body = it.body() ?: return@flatMap Err(EduCoreBundle.message("error.failed.to.parse.response"))
      val doc = Jsoup.parse(body.string())
      Ok(doc)
    }
  }

  fun login(userName: String, password: String): Result<CodeforcesAccount, String> = handlingDownloadException {
    if (userName.isEmpty() || password.isEmpty()) {
      return Err(EduCoreBundle.message("error.empty.handle.or.password"))
    }

    val (token, jSessionId) = getInstance().getCSRFTokenWithJSessionID().onError {
      return Err(it)
    }
    val loginResponse = getInstance().postLoginForm(userName, password, jSessionId, token).onError {
      return Err(it)
    }

    val htmlResponse = loginResponse.body()?.string() ?: return Err(EduCoreBundle.message("error.unknown.error"))

    if (htmlResponse.contains("Invalid handle/email or password")) {
      return Err(EduCoreBundle.message("error.invalid.handle.or.password"))
    }

    if (loginResponse.isSuccessful) {
      var handle = Jsoup.parse(htmlResponse)
        .getElementsByTag("script")
        .map { handleRegex.find(it.data())?.destructured?.toList()?.firstOrNull() }
        .firstOrNull()

      if (handle == null) handle = getInstance().getProfile(jSessionId) ?: return Err(EduCoreBundle.message("error.unknown.error"))

      val userInfo = CodeforcesUserInfo()
      userInfo.handle = handle
      val account = CodeforcesAccount(userInfo)
      account.saveSessionId(jSessionId)
      account.savePassword(password)
      return Ok(account)
    }
    return Err(EduCoreBundle.message("error.unknown.error"))
  }

  fun getCSRFTokenWithJSessionID(): Result<Pair<String, String>, String> = handlingDownloadException {
    val loginPage = service.getLoginPage().executeParsingErrors().onError { return Err(it) }
    val loginPageBody = loginPage.body()?.string() ?: return Err(EduCoreBundle.message("error.failed.to.parse.response"))

    val body = Jsoup.parse(loginPageBody)
    val csrfToken = body.getElementsByClass("csrf-token").attr("data-csrf")

    val jSessionId = loginPage.headers().toMultimap()["set-cookie"]
                       ?.filter { it.contains("JSESSIONID") }
                       ?.joinToString("; ") { it.split(";")[0] }
                       ?.split("=")?.get(1) ?: return Err(EduCoreBundle.message("error.failed.to.parse.response"))
    return Ok(csrfToken to jSessionId)
  }


  private fun postLoginForm(
    handle: String,
    password: String,
    jSessionID: String,
    csrfToken: String
  ): Result<Response<ResponseBody>, String> = handlingDownloadException {
    return service.postLoginPage(
      csrfToken = csrfToken,
      handle = handle,
      password = password,
      cookie = "JSESSIONID=$jSessionID"
    ).executeParsingErrors()
  }

  fun submitSolution(
    task: CodeforcesTask,
    solution: String,
    account: CodeforcesAccount,
    project: Project
  ): Result<String, String> = handlingDownloadException {
    if ((!account.isUpToDate() || !isLoggedIn()) && !getInstance().updateJSessionID(account)) {
      return Err(EduFormatBundle.message("error.access.denied"))
    }

    val jSessionID = account.getSessionId() ?: return Err(EduFormatBundle.message("error.access.denied"))
    val contestId = task.course.id
    val languageCode = task.course.languageCode
    val programTypeId = task.course.programTypeId

    val submitPage = service.getSubmissionPage(
      contestId, languageCode, programTypeId, task.problemIndex,
      "JSESSIONID=$jSessionID"
    ).executeParsingErrors().onError {
      return Err(it)
    }
    val htmlPage = submitPage.body()?.string() ?: return Err(EduCoreBundle.message("error.unknown.error"))
    val body = Jsoup.parse(htmlPage)
    val csrfToken = body.getElementsByClass("csrf-token").attr("data-csrf")

    val response = service.postSolution(
      csrfToken = csrfToken,
      submittedProblemIndex = task.problemIndex,
      source = solution,
      contestId = contestId,
      programTypeId = programTypeId,
      csrf_token = csrfToken,
      cookie = "JSESSIONID=$jSessionID"
    ).executeParsingErrors().onError {
      return Err(it)
    }

    val responseBody = Jsoup.parse(response.body()?.string().orEmpty())

    responseBody.getElementsByClass("error for__source").forEach {
      if (it.text().contains("You have submitted exactly the same code before"))
        return Ok(EduCoreBundle.message("codeforces.error.you.have.submitted.code.before"))
      return Ok(it.text())
    }

    val cc = responseBody.getElementsByAttributeValue("name", "cc").attr("content")
    val pc = responseBody.getElementsByAttributeValue("name", "pc").attr("content")
    val timeStamp = System.currentTimeMillis()

    ApplicationManager.getApplication().executeOnPooledThread {
      connectToWebSocketWithTimeout(
        "wss://pubsub.codeforces.com/ws/s_$pc/s_$cc?_=$timeStamp&tag=&time=&eventid=",
        "s_$cc",
        csrfToken,
        jSessionID,
        project
      )
    }

    return Ok("")
  }

  private fun connectToWebSocketWithTimeout(url: String, channel: String, csrfToken: String, jSessionID: String, project: Project) {
    val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.MINUTES).build()
    val request = Request.Builder().url(url).build()
    val latch = CountDownLatch(1)
    val task = project.getCurrentTask() as? CodeforcesTask ?: return
    val socket = client.newWebSocket(request, object : WebSocketListener() {

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        LOG.debug("WS connection failure. StackTrace: ${t.stackTrace.joinToString("\n")}")
        latch.countDown()
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        LOG.debug("WS connection closed. Reason: $reason")
        latch.countDown()
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        val wsResponse = objectMapper.readValue(text, object : TypeReference<CodeforcesWSResponse>() {})
        if (channel == wsResponse.channel) {
          TaskToolWindowView.getInstance(project).checkStarted(task, true)
          val dataResponse = objectMapper.readValue(wsResponse.text, object : TypeReference<DataResponse>() {})

          val verdict = dataResponse.verdict
          val checkResult = CheckResult(verdict.toCheckStatus(), verdict.name.replace("_", " ").lowercase().capitalize())

          task.feedback = CheckFeedback(Date(), checkResult)
          task.status = verdict.toCheckStatus()
          ProjectView.getInstance(project).refresh()
          TaskToolWindowView.getInstance(project).checkFinished(task, checkResult)

          if (verdict != CodeforcesVerdict.TESTING) {
            YamlFormatSynchronizer.saveItem(task)
            latch.countDown()
          }
        }
      }
    })

    latch.await(TIMEOUT_IN_SEC, TimeUnit.SECONDS)
    socket.close(1000, "")
    client.dispatcher.executorService.shutdown()
    val submissions = getUserSubmissions(task.course.id, listOf(task), csrfToken, jSessionID)[task.id] ?: return
    if (submissions.isNotEmpty()) SubmissionsManager.getInstance(project).addToSubmissions(task.id, submissions[0])
  }

  fun getUserSubmissions(
    contestId: Int,
    tasks: List<CodeforcesTask>,
    csrfToken: String,
    jSessionID: String
  ): Map<Int, List<StepikBasedSubmission>> {
    if (CodeforcesSettings.getInstance().isLoggedIn()) {
      val body = service.getUserSolutions(CodeforcesSettings.getInstance().account!!.userInfo.handle, contestId)
        .executeParsingErrors().onError { return emptyMap() }.body()

      val submissionsByProblemIndex = body?.result?.groupBy { it.problem.index }

      return tasks.associate { task ->
        val taskSubmissions = submissionsByProblemIndex?.get(task.problemIndex)?.map {
          val course = task.course
          val mainFileName = course.configurator?.courseBuilder?.mainTemplateName(course)
          StepikBasedSubmission().apply {
            this.id = it.id
            this.time = Date.from(Instant.ofEpochSecond(it.creationTimeSeconds.toLong()))
            this.status = it.verdict.stringVerdict
            this.taskId = task.id
            val submissionSource = getSubmissionSource(it.id, csrfToken, jSessionID)

            if (mainFileName != null) {
              val solutionFile = SolutionFile(GeneratorUtils.joinPaths(task.sourceDir, mainFileName), submissionSource, true)
              reply = EduTaskReply().apply {
                solution = listOf(solutionFile)
              }
            }

          }
        }
        if (taskSubmissions == null) task.id to emptyList()
        else task.id to taskSubmissions
      }
    }
    return emptyMap()
  }

  private fun getSubmissionSource(submissionId: Int, token: String, jSessionId: String): String =
    service.getSubmissionSource(token, submissionId, "JSESSIONID=$jSessionId")
      .executeParsingErrors()
      .onError { return "" }
      .body()?.source ?: ""

  private fun isLoggedIn(): Boolean {
    CodeforcesSettings.getInstance().account?.getSessionId()?.let { getProfile(it) } ?: return false
    return true
  }

  private fun updateJSessionID(codeforcesAccount: CodeforcesAccount): Boolean {
    val (csrfToken, jSessionId) = getInstance().getCSRFTokenWithJSessionID().onError { return false }
    val password = codeforcesAccount.getPassword() ?: return false
    val loginResponse = getInstance().postLoginForm(codeforcesAccount.userInfo.handle, password, jSessionId, csrfToken)
      .onError { return false }
    val htmlResponse = loginResponse.body()?.string() ?: return false
    if (loginResponse.isSuccessful && !htmlResponse.contains("Invalid handle/email or password")) {
      codeforcesAccount.saveSessionId(jSessionId)
      codeforcesAccount.updateExpiresAt()
      return true
    }
    return false
  }

  private fun getProfile(jSessionID: String): String? {
    val response = service.profile("JSESSIONID=$jSessionID").executeParsingErrors().onError { return null }
    return response.raw().priorResponse
      ?.headers("location")
      ?.find { it.startsWith("https://codeforces.com/profile/") }
      ?.split("/")
      ?.last()
  }

  /**
   * Get mandatory data for contest registration
   * returns RegistrationData: CSRF token, TermsOfAgreement text and team registration ability
   */
  fun getRegistrationData(contestId: Int): ContestRegistrationData? {
    val account = CodeforcesSettings.getInstance().account ?: return null
    if ((!account.isUpToDate() || !isLoggedIn()) && !getInstance().updateJSessionID(account)) {
      return null
    }
    val jSessionID = account.getSessionId() ?: return null

    val registrationPage = service.getRegistrationPage(contestId, "JSESSIONID=$jSessionID")
                             .executeParsingErrors()
                             .onError { return null }.body() ?: return null

    val doc = Jsoup.parse(registrationPage.string())
    val csrfToken = doc.getElementsByClass("csrf-token").attr("data-csrf")
    val text = doc.getElementsByClass("terms").firstOrNull()?.text() ?: return null
    val isTeamRegistrationAvailable = doc.getElementsByAttributeValue("id", "takePartAsTeamInput").isNotEmpty()

    return ContestRegistrationData(csrfToken, text, isTeamRegistrationAvailable)
  }

  /**
   * NB! Accepts CSRF token only from the registration page
   */
  fun registerToContest(contestId: Int, csrfToken: String): Boolean {
    val jSessionId = CodeforcesSettings.getInstance().account?.getSessionId() ?: return false

    val response = service.postRegistration(csrfToken, contestId, "JSESSIONID=$jSessionId")
      .executeParsingErrors()
      .onError { return false }

    val containsSuccessRegisterText = response.body()?.string()?.contains("You have been successfully registered") ?: return false
    return response.raw().isSuccessful && containsSuccessRegisterText
  }

  fun isUserRegisteredForContest(contestId: Int): Boolean {
    val jSessionId = CodeforcesSettings.getInstance().account?.getSessionId() ?: return false
    val registrationData = service.getContestRegistrationData(contestId, "JSESSIONID=$jSessionId")
                             .executeParsingErrors()
                             .onError { return false }.body() ?: return false

    val doc = Jsoup.parse(registrationData.string())
    return doc.getElementsByClass("welldone").isNotEmpty()
  }

  companion object {

    private val LOG = Logger.getInstance(CodeforcesConnector::class.java)

    fun getInstance(): CodeforcesConnector = service()
  }
}

data class ContestRegistrationData(val token: String, val termsOfAgreement: String, val isTeamRegistrationAvailable: Boolean)

private inline fun <T> handlingDownloadException(fn: () -> Result<T, String>): Result<T, String> {
  return try {
    fn()
  }
  catch (e: CoursesDownloadingException) {
    Err(e.uiMessage)
  }
}