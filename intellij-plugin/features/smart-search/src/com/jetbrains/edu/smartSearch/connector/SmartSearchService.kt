package com.jetbrains.edu.smartSearch.connector

import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.http.Body
import retrofit2.http.POST

interface SmartSearchService {
  @POST("/search")
  suspend fun search(@Body query: SearchRequestBody): List<CourseTaskData>

  data class SearchRequestBody(
    @JsonProperty("query")
    val query: String,
    @JsonProperty("collection_name")
    val collectionName: String,
    @JsonProperty("limit")
    val numberOfDocuments: Int
  )

  data class CourseTaskData(
    @JsonProperty("course_name")
    val courseName: String,
    @JsonProperty("marketplace_id")
    val marketplaceId: Int,
    @JsonProperty("task_id")
    val taskId: Int,
    @JsonProperty("task_name")
    val taskName: String,
    @JsonProperty("update_version")
    val updateVersion: Int
  )
}