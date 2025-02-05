package com.jetbrains.edu.smartSearch.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import okhttp3.ConnectionPool
import java.net.HttpURLConnection.*

@Service(Service.Level.APP)
class SmartSearchConnector {
  private val url: String
    get() = "http://localhost:8000"

  private val connectionPool = ConnectionPool()

  private val service: SmartSearchService
    get() = createSmartSearchService()

  @Throws(IllegalStateException::class)
  private fun createSmartSearchService(): SmartSearchService {
    return createRetrofitBuilder(url, connectionPool)
      .addConverterFactory(SmartSearchConverterFactory())
      .build()
      .create(SmartSearchService::class.java)
  }

  suspend fun search(
    query: String,
    collectionName: String,
    numberOfDocuments: Int
  ): List<SmartSearchService.CourseTaskData> {
    return service.search(SmartSearchService.SearchRequestBody(query, collectionName, numberOfDocuments))
  }

  companion object {
    fun getInstance(): SmartSearchConnector = service()
  }
}