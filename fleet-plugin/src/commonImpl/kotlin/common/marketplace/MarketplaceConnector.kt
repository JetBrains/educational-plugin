package fleet.edu.common.marketplace

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_META_FILE
import com.jetbrains.edu.learning.createRetrofitBuilder
import com.jetbrains.edu.learning.executeHandlingExceptions
import com.jetbrains.edu.learning.json.readCourseJson
import com.jetbrains.edu.learning.marketplace.api.MarketplaceRepositoryEndpoints
import com.jetbrains.edu.learning.marketplace.api.QueryData
import com.jetbrains.edu.learning.marketplace.api.UpdateInfo
import fleet.net.HttpClientApi
import fleet.net.downloadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import retrofit2.converter.jackson.JacksonConverterFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext


object MarketplaceConnector {
  private val connectionPool = ConnectionPool()
  private val objectMapper: ObjectMapper = createMapper()
  private val converterFactory: JacksonConverterFactory = JacksonConverterFactory.create(objectMapper)

  private const val repositoryUrl = "https://plugins.jetbrains.com"

  val endpoints: MarketplaceRepositoryEndpoints by lazy {
    val retrofit = createRetrofitBuilder(repositoryUrl, connectionPool)
      .addConverterFactory(converterFactory)
      .build()

    retrofit.create(MarketplaceRepositoryEndpoints::class.java)
  }

  private fun createMapper(): ObjectMapper {
    val objectMapper = ObjectMapper()
    objectMapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_SETTERS)
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    return objectMapper
  }

  private fun getLatestCourseUpdateInfo(courseId: Int): UpdateInfo? {
    val response = endpoints.getUpdateId(QueryData(GraphqlQuery.lastUpdateId(courseId))).executeHandlingExceptions()
    val updateInfoList = response?.body()?.data?.updates?.updateInfoList
    if (updateInfoList == null) {
      error("Update info list for course $courseId is null")
    }
    else {
      return updateInfoList.firstOrNull()
    }
  }

  private fun getLatestUpdateId(courseId: Int): Int {
    val updateInfo = getLatestCourseUpdateInfo(courseId)
    if (updateInfo == null) {
      error("Update info for course $courseId is null")
    }
    return updateInfo.updateId
  }

  suspend fun loadCourse(courseId: Int, callback: (course: Course) -> Unit) {
    val url = "$repositoryUrl/plugin/download?updateId=${getLatestUpdateId(courseId)}"
    val tempFile = withContext(Dispatchers.IO) { Files.createTempFile("marketplace-$courseId", ".zip") }
    val httpClient = coroutineContext[HttpClientApi]?.httpClient() ?: error("must have an HttpClientApi in coroutine context")
    httpClient.downloadFile(url, tempFile)
    withContext(Dispatchers.IO) {
      ZipFile(tempFile.toFile()).use { zipFile ->
        val entry = zipFile.getEntry(COURSE_META_FILE)
        val reader = { zipFile.getInputStream(entry).reader(StandardCharsets.UTF_8) }
        val course = readCourseJson(reader) ?: return@withContext
        callback(course)
      }
    }
  }
}
