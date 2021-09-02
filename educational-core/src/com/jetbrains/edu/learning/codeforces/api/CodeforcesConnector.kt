package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getLanguages
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import okhttp3.ConnectionPool
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory

abstract class CodeforcesConnector {
  @VisibleForTesting
  val objectMapper: ObjectMapper
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory

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

  fun getCSRFTokenWithJSessionID(): Result<Pair<String, String>, String> {
    val loginPage = service.getLoginPage().executeParsingErrors()
      .onError { return Err(it) }
    loginPage.body() ?: return Err(EduCoreBundle.message("error.failed.to.parse.response"))

    val body = Jsoup.parse(loginPage.body()!!.string())
    val csrfToken = body.getElementsByClass("csrf-token").attr("data-csrf")

    val jSessionId = loginPage.headers().toMultimap()["set-cookie"]!!.filter { it.contains("JSESSIONID") }.joinToString(
      "; ") { it.split(";")[0] }.split("=")[1]
    return Ok(Pair(csrfToken, jSessionId))
  }


  fun postLoginForm(handle: String, password: String, jSessionID: String, csrfToken: String): Result<Response<ResponseBody>, String> {
    return service.postLoginPage(csrfToken = csrfToken,
                                 handle = handle,
                                 password = password,
                                 cookie = "JSESSIONID=$jSessionID").executeParsingErrors()
  }

  fun updateJSessionID(handle: String): Boolean {
    val (csrfToken, jSessionId) = getInstance().getCSRFTokenWithJSessionID().onError { return false }
    val credentialAttributes = CredentialAttributes(generateServiceName(CodeforcesNames.CODEFORCES_SUBSYSTEM_NAME, handle))
    val password = PasswordSafe.instance.get(credentialAttributes)?.getPasswordAsString()
    password ?: return false
    val loginResponse = getInstance().postLoginForm(handle, password, jSessionId, csrfToken).onError { return false }
    return loginResponse.isSuccessful && !loginResponse.body()!!.string().contains("Invalid handle/email or password")
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