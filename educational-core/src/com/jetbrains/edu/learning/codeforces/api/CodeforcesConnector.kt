package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.checkStatusCode
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestName
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getLanguages
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_URL
import com.jetbrains.edu.learning.codeforces.ContestShortInfo
import com.jetbrains.edu.learning.codeforces.ContestURLInfo
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.executeHandlingExceptions
import okhttp3.ConnectionPool
import org.jsoup.Jsoup
import retrofit2.converter.jackson.JacksonConverterFactory

class CodeforcesConnector {
  @VisibleForTesting
  val objectMapper: ObjectMapper
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory

  init {
    val module = SimpleModule()
    objectMapper = createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private val service: CodeforcesService by lazy { service() }

  private fun service(): CodeforcesService =
    createRetrofitBuilder(CODEFORCES_URL, connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(CodeforcesService::class.java)

  fun getContests(withTrainings: Boolean = false): ContestsList? =
    service.contests(withTrainings)
      .executeHandlingExceptions()
      ?.checkStatusCode()
      ?.body()

  fun getContestInfo(contestURLInfo: ContestURLInfo): CodeforcesCourse? {
    val response = service.problems(contestURLInfo.id, contestURLInfo.locale)
                     .executeHandlingExceptions()
                     ?.checkStatusCode()
                     ?.body() ?: return null
    return CodeforcesCourse(contestURLInfo, response)
  }

  fun getContestShortInfo(contestId: Int): ContestShortInfo? {
    val response = service.status(contestId)
                     .executeHandlingExceptions()
                     ?.checkStatusCode()
                     ?.body() ?: return null
    val doc = Jsoup.parse(response.string())
    return ContestShortInfo(getContestName(doc), getLanguages(doc))
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