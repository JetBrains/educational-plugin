package com.jetbrains.edu.tools.marketplace

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

private const val GRAPHQL_ENDPOINT = "https://plugins.jetbrains.com/api/search/graphql"
private const val DOWNLOAD_ENDPOINT = "https://plugins.jetbrains.com/plugin/download"
private const val LOADING_STEP = 24
private const val UPDATES_BATCH_SIZE = 20
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
private val DISALLOWED_FILE_CHARS = Regex("[^a-zA-Z0-9._-]+")

private data class QueryData(val query: String)
private data class GraphqlResponse<T>(val data: T?)
private data class SearchData(val plugins: PluginsData)
private data class PluginsData(val total: Int, val plugins: List<CourseDto>)
private data class CourseDto(val id: Int, val name: String)
private data class UpdatesData(val updates: UpdatesInfo)
private data class UpdatesInfo(val updates: List<UpdateDto>)
private data class UpdateDto(val id: Int, val pluginId: Int, val version: String)
private data class CourseWithUpdate(val id: Int, val name: String, val updateId: Int, val updateVersion: String)

private class MarketplaceClient(
  private val httpClient: OkHttpClient,
  private val objectMapper: ObjectMapper
) {
  fun loadAllPublicCourses(): List<CourseDto> {
    val courses = mutableListOf<CourseDto>()
    var offset = 0
    var total = Int.MAX_VALUE

    while (offset < total) {
      val query = publicCoursesQuery(offset, LOADING_STEP)
      val response: GraphqlResponse<SearchData> = postGraphql(query)
      val pluginsData = response.data?.plugins
        ?: throw IOException("Marketplace GraphQL response doesn't contain plugins data")

      if (pluginsData.plugins.isEmpty()) {
        break
      }

      total = pluginsData.total
      courses += pluginsData.plugins
      offset += LOADING_STEP
    }

    return courses
  }

  fun loadLatestUpdates(courseIds: List<Int>): Map<Int, UpdateDto> {
    if (courseIds.isEmpty()) return emptyMap()

    val updatesByCourseId = mutableMapOf<Int, UpdateDto>()
    for (batch in courseIds.chunked(UPDATES_BATCH_SIZE)) {
      val query = updatesByCourseIdsQuery(batch)
      val response: GraphqlResponse<UpdatesData> = postGraphql(query)
      val updates = response.data?.updates?.updates
        ?: throw IOException("Marketplace GraphQL response doesn't contain updates data")

      for (update in updates) {
        updatesByCourseId.putIfAbsent(update.pluginId, update)
      }
    }

    return updatesByCourseId
  }

  fun downloadCourseArchive(updateId: Int, buildNumber: String, source: String, uuid: UUID, targetFile: Path) {
    val encodedBuild = URLEncoder.encode(buildNumber, Charsets.UTF_8)
    val url = "$DOWNLOAD_ENDPOINT?updateId=$updateId&uuid=$uuid&build=$encodedBuild&source=$source"
    val request = Request.Builder().url(url).get().build()

    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("Failed to download update $updateId: HTTP ${response.code}")
      }

      val responseBody = response.body ?: throw IOException("Empty response body for update $updateId")
      Files.copy(responseBody.byteStream(), targetFile, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private inline fun <reified T> postGraphql(query: String): T {
    val requestBody = objectMapper.writeValueAsString(QueryData(query)).toRequestBody(JSON_MEDIA_TYPE)
    val request = Request.Builder()
      .url(GRAPHQL_ENDPOINT)
      .post(requestBody)
      .build()

    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        val errorBody = response.body?.string()?.takeIf { it.isNotBlank() } ?: "<empty>"
        throw IOException("Marketplace GraphQL request failed: HTTP ${response.code}. Response: $errorBody")
      }
      val body = response.body?.string() ?: throw IOException("Marketplace GraphQL response body is empty")
      return objectMapper.readValue(body)
    }
  }
}

private fun publicCoursesQuery(offset: Int, max: Int): String =
  """
  query {
    plugins(
      search: {
        filters: [{ field: "family", value: "edu" }, { field: "fields.isPrivate", value: false }]
        max: $max
        offset: $offset
        sortBy: RATING
      }
    ) {
      total
      plugins {
        id
        name
      }
    }
  }
  """.trimIndent()

private fun updatesByCourseIdsQuery(courseIds: List<Int>): String =
  """
  query {
    updates(
      search: {
        filters: [{ field: "pluginId", value: [${courseIds.joinToString()}] }]
        max: $UPDATES_BATCH_SIZE
        collapseField: PLUGIN_ID
      }
    ) {
      updates {
        id
        pluginId
        version
      }
    }
  }
  """.trimIndent()

private fun sanitizeForFileName(value: String): String {
  val sanitized = value
    .replace(DISALLOWED_FILE_CHARS, "_")
    .replace("_+".toRegex(), "_")
    .trim('_')
  return sanitized.ifEmpty { "course" }
}

private fun parseArgs(args: Array<String>): Config {
  var outputDir: Path = Path.of("/home/iliaposov/Documents/tickets/courses")
  var buildNumber = "261.22158"

  var index = 0
  while (index < args.size) {
    when (args[index]) {
      "--output" -> {
        outputDir = Path.of(args.getOrNull(index + 1) ?: error("Missing value for --output"))
        index += 2
      }
      "--build" -> {
        buildNumber = args.getOrNull(index + 1) ?: error("Missing value for --build")
        index += 2
      }
      else -> error("Unknown argument: ${args[index]}. Supported args: --output <path>, --build <build>")
    }
  }

  return Config(outputDir, buildNumber)
}

private data class Config(val outputDir: Path, val buildNumber: String)

fun main(args: Array<String>) {
  val config = parseArgs(args)

  Files.createDirectories(config.outputDir)

  val client = MarketplaceClient(
    OkHttpClient.Builder().build(),
    ObjectMapper().registerModule(KotlinModule.Builder().build())
  )

  val courses = client.loadAllPublicCourses()
  val updatesByCourseId = client.loadLatestUpdates(courses.map { it.id })

  val coursesWithUpdates = courses.mapNotNull { course ->
    val update = updatesByCourseId[course.id]
    if (update == null) {
      println("Skip course ${course.id} (${course.name}): no update info")
      null
    }
    else {
      CourseWithUpdate(course.id, course.name, update.id, update.version)
    }
  }

  println("Found ${coursesWithUpdates.size} public courses with updates")

  val uuid = UUID.randomUUID()
  val source = "other"
  var downloaded = 0
  var skipped = 0

  for (course in coursesWithUpdates) {
    val fileName = sanitizeForFileName("${course.id}_${course.name}_u${course.updateVersion}") + ".zip"
    val outputFile = config.outputDir.resolve(fileName)

    if (Files.exists(outputFile)) {
      skipped++
      println("Skip existing: ${outputFile.fileName}")
      continue
    }

    client.downloadCourseArchive(course.updateId, config.buildNumber, source, uuid, outputFile)
    downloaded++
    println("Downloaded: ${outputFile.fileName}")
  }

  println("Done. Downloaded: $downloaded, skipped existing: $skipped, total: ${coursesWithUpdates.size}")
}
