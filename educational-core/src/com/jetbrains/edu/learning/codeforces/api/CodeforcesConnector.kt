package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getLanguages
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesAccount
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesUserInfo
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import okhttp3.ConnectionPool
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection

abstract class CodeforcesConnector {
  @VisibleForTesting
  val objectMapper: ObjectMapper
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  private val handleRegex = """var handle = "([\w\-]+)"""".toRegex()

  init {
    val module = SimpleModule()
    objectMapper = createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  protected abstract val baseUrl: String

  private val service: CodeforcesService by lazy { service() }

  private fun service(): CodeforcesService =
    createRetrofitBuilder(baseUrl, connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(CodeforcesService::class.java)

  fun getContests(withTrainings: Boolean = false, locale: String = "en"): ContestsList? =
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
    val contestInfo = contestsList.contests.find { it.id == contestId }
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

  fun login(userName: String, password: String): Boolean {
    if (userName.isEmpty() || password.isEmpty()) {
      return loginErrorMessage(EduCoreBundle.message("error.empty.handle.or.password"))
    }

    val (token, jSessionId) = getInstance().getCSRFTokenWithJSessionID().onError {
      return loginErrorMessage(it)
    }
    val loginResponse = getInstance().postLoginForm(userName, password, jSessionId, token).onError {
      return loginErrorMessage(it)
    }

    val htmlResponse = loginResponse.body()?.string() ?: return false

    if (htmlResponse.contains("Invalid handle/email or password")) {
      return loginErrorMessage(EduCoreBundle.message("error.invalid.handle.or.password"))
    }

    if (loginResponse.isSuccessful) {
      var handle = Jsoup.parse(htmlResponse)
        .getElementsByTag("script")
        .map { handleRegex.find(it.data())?.destructured?.toList()?.firstOrNull() }
        .firstOrNull()

      if (handle == null) handle = getInstance().getProfile(jSessionId) ?: return false

      val userInfo = CodeforcesUserInfo()
      userInfo.handle = handle
      val account = CodeforcesAccount(userInfo)
      account.saveSessionId(jSessionId)
      account.savePassword(password)
      CodeforcesSettings.getInstance().account = account
      return true
    }
    return loginErrorMessage(EduCoreBundle.message("error.unknown.error"))
  }

  private fun loginErrorMessage(message: String): Boolean {
    Messages.showErrorDialog(message, EduCoreBundle.message("error.login.error.title"))
    return false
  }

  fun getCSRFTokenWithJSessionID(): Result<Pair<String, String>, String> {
    val loginPage = service.getLoginPage().executeParsingErrors()
      .onError { return Err(it) }
    loginPage.body() ?: return Err(EduCoreBundle.message("error.failed.to.parse.response"))

    val body = Jsoup.parse(loginPage.body()?.string())
    val csrfToken = body.getElementsByClass("csrf-token").attr("data-csrf")

    val jSessionId = loginPage.headers().toMultimap()["set-cookie"]
                       ?.filter { it.contains("JSESSIONID") }
                       ?.joinToString("; ") { it.split(";")[0] }
                       ?.split("=")?.get(1) ?: return Err(EduCoreBundle.message("error.failed.to.parse.response"))
    return Ok(Pair(csrfToken, jSessionId))
  }


  fun postLoginForm(handle: String, password: String, jSessionID: String, csrfToken: String): Result<Response<ResponseBody>, String> {
    return service.postLoginPage(csrfToken = csrfToken,
                                 handle = handle,
                                 password = password,
                                 cookie = "JSESSIONID=$jSessionID").executeParsingErrors()
  }

  fun submitSolution(task: CodeforcesTask, solution: String, account: CodeforcesAccount): Result<Boolean, String> {

    if (!account.isUpToDate() && !getInstance().updateJSessionID(account)) {
      return Err(EduCoreBundle.message("codeforces.failed.to.submit.solution"))
    }

    val jSessionID = account.getSessionId()
    val contestId = task.course.id
    val languageCode = task.course.languageCode
    val programTypeId = CodeforcesTask.codeforcesProgramTypeId(task.course as CodeforcesCourse)?.toString()
    val submittedProblemIndex = task.presentableName.substringBefore(".")

    val submitPage = service.getSubmissionPage(contestId, languageCode, programTypeId, submittedProblemIndex,
                                               "JSESSIONID=$jSessionID").executeParsingErrors().onError {
      return Err(EduCoreBundle.message("error.failed.to.load.submission.page"))
    }
    val htmlPage = submitPage.body()?.string() ?: return Err(EduCoreBundle.message("codeforces.failed.to.submit.solution"))
    val body = Jsoup.parse(htmlPage)
    val csrfToken = body.getElementsByClass("csrf-token").attr("data-csrf")

    val response = service.postSolution(csrfToken = csrfToken,
                                        submittedProblemIndex = submittedProblemIndex,
                                        source = solution,
                                        contestId = contestId,
                                        programTypeId = programTypeId,
                                        csrf_token = csrfToken,
                                        cookie = "JSESSIONID=$jSessionID").executeParsingErrors().onError {
      return Err(EduCoreBundle.message("error.unknown.error"))
    }
    return Ok(response.isSuccessful && response.raw().priorResponse()?.code() == HttpURLConnection.HTTP_MOVED_TEMP)
  }

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

  fun getProfile(jSessionID: String): String? {
    val response = service.profile("JSESSIONID=$jSessionID").executeParsingErrors().onError { return null }
    return response.raw().priorResponse()
      ?.headers("location")
      ?.find { it.startsWith("https://codeforces.com/profile/") }
      ?.split("/")
      ?.last()
  }

  companion object {
    @JvmStatic
    fun getInstance(): CodeforcesConnector = service()

    @JvmStatic
    private fun createMapper(module: SimpleModule): ObjectMapper {
      val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

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