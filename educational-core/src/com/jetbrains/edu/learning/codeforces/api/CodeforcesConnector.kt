package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getLanguages
import com.jetbrains.edu.learning.codeforces.ContestInformation
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import okhttp3.ConnectionPool
import org.jsoup.Jsoup
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
      val responseBody = it.body() ?: return@flatMap Err(EduCoreErrorBundle.message("failed.to.parse.response"))
      val doc = Jsoup.parse(responseBody.string())
      Ok(CodeforcesCourse(contestParameters, doc))
    }

  fun getContestInformation(contestId: Int): Result<ContestInformation, String> {
    val contestsList = getContests() ?: return Err(EduCoreErrorBundle.message("codeforces.failed.to.get.contests.list"))
    val contestInfo = contestsList.contests.find { it.id == contestId }
                      ?: return Err(EduCoreErrorBundle.message("codeforces.failed.to.find.contest.in.contests.list"))

    val responseBody = service.status(contestId).executeParsingErrors()
                         .onError { return Err(it) }
                         .body() ?: return Err(EduCoreErrorBundle.message("failed.to.parse.response"))
    val doc = Jsoup.parse(responseBody.string())
    val contestLanguage = getLanguages(doc) ?: return Err(EduCoreErrorBundle.message("codeforces.failed.to.get.contest.language"))

    return Ok(ContestInformation(contestId, contestInfo.name, contestLanguage, contestInfo.endTime))
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