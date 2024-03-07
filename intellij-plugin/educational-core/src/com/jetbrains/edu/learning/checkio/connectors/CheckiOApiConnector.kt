package com.jetbrains.edu.learning.checkio.connectors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.checkio.api.CheckiOApiEndpoint
import com.jetbrains.edu.learning.checkio.api.executeHandlingCheckiOExceptions
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOStation
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory

abstract class CheckiOApiConnector(private val oauthConnector: CheckiOOAuthConnector) {
  abstract val languageId: String
  abstract val baseUrl: String

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  private val objectMapper: ObjectMapper
  private val checkiOApiEndpoint: CheckiOApiEndpoint by lazy { checkiOApiEndpoint() }

  init {
    val module = SimpleModule()
    objectMapper = ConnectorUtils.createRegisteredMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private fun checkiOApiEndpoint(): CheckiOApiEndpoint =
    createRetrofitBuilder(baseUrl, connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(CheckiOApiEndpoint::class.java)

  open fun getMissionList(): List<CheckiOMission> {
    val accessToken = oauthConnector.getAccessToken()
    val response = checkiOApiEndpoint.getMissionList(accessToken).executeHandlingCheckiOExceptions()
    val missionBeans = response.checkiOMissions
    val missions = missionBeans.map { bean ->
      val station = CheckiOStation()
      station.apply {
        id = bean.stationId
        name = bean.stationName
      }

      CheckiOMission().apply {
        this.station = station
        id = bean.id
        name = bean.title
        descriptionFormat = DescriptionFormat.HTML
        descriptionText = bean.description
        status = if (bean.isSolved) CheckStatus.Solved else CheckStatus.Unchecked
        code = bean.code
        slug = bean.slug
        secondsFromLastChangeOnServer = bean.secondsPast ?: Long.MAX_VALUE
      }
    }
    return missions
  }
}