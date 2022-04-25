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
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.ConnectorUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getLanguages
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesAccount
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesUserInfo
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
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

  private val service: CodeforcesService by lazy { service() }

  private fun service(): CodeforcesService =
    createRetrofitBuilder(baseUrl, connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(CodeforcesService::class.java)

  fun getContests(withTrainings: Boolean = false, locale: String = "en"): ContestsResponse? =
    service.contests(withTrainings, locale)
      .executeHandlingExceptions()
      ?.checkStatusCode()
      ?.body()

  fun getContest(contestParameters: ContestParameters): Result<CodeforcesCourse, String> =
    service.problems(contestParameters.id, contestParameters.locale).executeParsingErrors().flatMap {
      val responseBody = it.body() ?: return@flatMap Err(EduCoreBundle.message("error.failed.to.parse.response"))
      val doc = Jsoup.parse(responseBody.string())
      Ok(CodeforcesCourse(contestParameters, doc))
    }

  fun getContestInformation(contestId: Int): Result<CodeforcesCourse, String> {
    val contestsList = getContests() ?: return Err(EduCoreBundle.message("codeforces.error.failed.to.get.contests.list"))
    val contestInfo = contestsList.result.find { it.id == contestId }
                      ?: return Err(EduCoreBundle.message("codeforces.error.failed.to.find.contest.in.contests.list"))

    val responseBody = service.status(contestId).executeParsingErrors()
                         .onError { return Err(it) }

                         .body() ?: return Err(EduCoreBundle.message("error.failed.to.parse.response"))
    val doc = Jsoup.parse(responseBody.string())
    val contestLanguages = getLanguages(doc) ?: return Err(EduCoreBundle.message("codeforces.error.failed.to.get.contest.language"))

    val contestParameters = ContestParameters(contestId, name = contestInfo.name, endDateTime = contestInfo.endTime,
                                              availableLanguages = contestLanguages)
    return Ok(CodeforcesCourse(contestParameters))
  }

  fun getContestsPage(): Result<Document, String> {
    return service.contestsPage().executeParsingErrors().flatMap {
      val body = it.body() ?: return@flatMap Err(EduCoreBundle.message("error.failed.to.parse.response"))
      val doc = Jsoup.parse(body.string())
      Ok(doc)
    }
  }

  @Suppress("UNNECESSARY_SAFE_CALL")
  fun login(userName: String, password: String): Result<CodeforcesAccount, String> {
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

  @Suppress("UNNECESSARY_SAFE_CALL")
  fun getCSRFTokenWithJSessionID(): Result<Pair<String, String>, String> {
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


  fun postLoginForm(handle: String, password: String, jSessionID: String, csrfToken: String): Result<Response<ResponseBody>, String> {
    return service.postLoginPage(csrfToken = csrfToken,
                                 handle = handle,
                                 password = password,
                                 cookie = "JSESSIONID=$jSessionID").executeParsingErrors()
  }

  @Suppress("UNNECESSARY_SAFE_CALL")
  fun submitSolution(task: CodeforcesTask, solution: String, account: CodeforcesAccount, project: Project): Result<String, String> {
    if ((!account.isUpToDate() || !isLoggedIn()) && !getInstance().updateJSessionID(account)) {
      return Err(EduCoreBundle.message("error.access.denied"))
    }

    val jSessionID = account.getSessionId() ?: return Err(EduCoreBundle.message("error.access.denied"))
    val contestId = task.course.id
    val languageCode = task.course.languageCode
    val programTypeId = (task.course as CodeforcesCourse).programTypeId
    val submittedProblemIndex = task.presentableName.substringBefore(".")

    val submitPage = service.getSubmissionPage(contestId, languageCode, programTypeId, submittedProblemIndex,
                                               "JSESSIONID=$jSessionID").executeParsingErrors().onError {
      return Err(it)
    }
    val htmlPage = submitPage.body()?.string() ?: return Err(EduCoreBundle.message("error.unknown.error"))
    val body = Jsoup.parse(htmlPage)
    val csrfToken = body.getElementsByClass("csrf-token").attr("data-csrf")

    val response = service.postSolution(csrfToken = csrfToken,
                                        submittedProblemIndex = submittedProblemIndex,
                                        source = solution,
                                        contestId = contestId,
                                        programTypeId = programTypeId,
                                        csrf_token = csrfToken,
                                        cookie = "JSESSIONID=$jSessionID").executeParsingErrors().onError {
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
      connectToWebSocketWithTimeout("wss://pubsub.codeforces.com/ws/s_$pc/s_$cc?_=$timeStamp&tag=&time=&eventid=",
                                    "s_$cc",
                                    csrfToken,
                                    jSessionID,
                                    project)
    }

    return Ok("")
  }

  private fun connectToWebSocketWithTimeout(url: String, channel: String, csrfToken: String, jSessionID: String, project: Project) {
    val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.MINUTES).build()
    val request = Request.Builder().url(url).build()
    val latch = CountDownLatch(1)
    val task = EduUtils.getCurrentTask(project) as? CodeforcesTask ?: return
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
          TaskDescriptionView.getInstance(project).checkStarted(task, true)
          val dataResponse = objectMapper.readValue(wsResponse.text, object : TypeReference<DataResponse>() {})

          val verdict = dataResponse.verdict
          val checkResult = CheckResult(verdict.toCheckStatus(), verdict.name.replace("_", " ").toLowerCase().capitalize())

          task.feedback = CheckFeedback(Date(), checkResult)
          task.status = verdict.toCheckStatus()
          ProjectView.getInstance(project).refresh()
          TaskDescriptionView.getInstance(project).checkFinished(task, checkResult)

          if (verdict != CodeforcesVerdict.TESTING) {
            YamlFormatSynchronizer.saveItem(task)
            latch.countDown()
          }
        }
      }
    })

    latch.await(TIMEOUT_IN_SEC, TimeUnit.SECONDS)
    socket.close(1000, "")
    client.dispatcher().executorService().shutdown()
    val submissions = getUserSubmissions(task.course.id, listOf(task), csrfToken, jSessionID)[task.id] ?: return
    if (submissions.isNotEmpty()) SubmissionsManager.getInstance(project).addToSubmissions(task.id, submissions[0])
  }

  @Suppress("UNNECESSARY_SAFE_CALL")
  fun getUserSubmissions(contestId: Int, tasks: List<Task>, csrfToken: String, jSessionID: String): Map<Int, List<StepikBasedSubmission>> {
    if (CodeforcesSettings.getInstance().isLoggedIn()) {
      val body = service.getUserSolutions(CodeforcesSettings.getInstance().account!!.userInfo.handle, contestId)
        .executeParsingErrors().onError { return emptyMap() }.body()

      val submissionsByNames = body?.result?.groupBy { "${it.problem.index}. ${it.problem.name}" }

      return tasks.associate { task ->
        val taskSubmissions = submissionsByNames?.get(task.name)?.map {
          val mainFileName = task.course.configurator?.courseBuilder?.mainTemplateName
          StepikBasedSubmission().apply {
            this.id = it.id
            this.time = Date.from(Instant.ofEpochSecond(it.creationTimeSeconds.toLong()))
            this.status = it.verdict.stringVerdict
            this.taskId = task.id
            val submissionSource = getSubmissionSource(it.id, csrfToken, jSessionID)

            if (mainFileName != null) {
              val solutionFile = SolutionFile(GeneratorUtils.joinPaths(task.sourceDir, mainFileName), submissionSource, true)
              reply = Reply().apply {
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

  @Suppress("UNNECESSARY_SAFE_CALL")
  fun getSubmissionSource(submissionId: Int, token: String, jSessionId: String): String =
    service.getSubmissionSource(token, submissionId, "JSESSIONID=$jSessionId")
      .executeParsingErrors()
      .onError { return "" }
      .body()?.source ?: ""

  private fun isLoggedIn(): Boolean {
    CodeforcesSettings.getInstance().account?.getSessionId()?.let { getProfile(it) } ?: return false
    return true
  }

  @Suppress("UNNECESSARY_SAFE_CALL")
  fun updateJSessionID(codeforcesAccount: CodeforcesAccount): Boolean {
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

  @Suppress("UNNECESSARY_SAFE_CALL")
  fun getProfile(jSessionID: String): String? {
    val response = service.profile("JSESSIONID=$jSessionID").executeParsingErrors().onError { return null }
    return response.raw().priorResponse()
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
    return response.raw().isSuccessful
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

    @JvmStatic
    fun getInstance(): CodeforcesConnector = service()
  }
}

data class ContestRegistrationData(val token: String, val termsOfAgreement: String, val isTeamRegistrationAvailable: Boolean)