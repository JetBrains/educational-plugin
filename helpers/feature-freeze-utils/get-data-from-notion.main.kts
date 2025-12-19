@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.17.2")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
@file:Import("shared-utils.main.kts")

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.system.exitProcess

val notionApiKey: String = System.getenv("NOTION_API_KEY") ?: error("Missing NOTION_API_KEY environment variable")
val notionDbId: String = System.getenv("NOTION_DB_ID") ?: error("Missing NOTION_DB_ID environment variable")

val releaseVersion: String = args.firstOrNull() ?: run {
  println("Error: RELEASE_VERSION is required. Pass it as the first argument.\nUsage: kotlinc -script update-plugin-version-info.main.kts <RELEASE_VERSION>")
  exitProcess(1)
}

val numberColumn = "Release"
val responsibleSlackColumn = "Slack ID"
val skipColumn = "Note"

data class QueryResponse(val results: List<Page> = emptyList())

data class Page(val properties: Map<String, Property> = emptyMap())

data class Property(
  val type: String? = null,
  val title: List<RichText>? = null,
  @param:JsonProperty("rich_text") val richText: List<RichText>? = null,
  val select: SelectOption? = null,
  @param:JsonProperty("multi_select") val multiSelect: List<SelectOption>? = null,
  val number: Double? = null,
  val checkbox: Boolean? = null,
  val date: DateValue? = null,
  val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RichText(@param:JsonProperty("plain_text") val plainText: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SelectOption(val name: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DateValue(val start: String? = null)

fun queryDatabase(databaseId: String): List<Page> {
  // Query with filter and sorting to ensure consistent order
  val filterBody = """
    {
        "sorts": [
            {
                "property": "$numberColumn",
                "direction": "ascending"
            },
            {
                "timestamp": "created_time",
                "direction": "ascending"
            }
        ]
    }
    """.trimIndent()

  val request = Request.Builder()
    .url("https://api.notion.com/v1/databases/$databaseId/query")
    .post(filterBody.toRequestBody("application/json".toMediaType()))
    .addHeader("Authorization", "Bearer $notionApiKey")
    .addHeader("Notion-Version", "2022-06-28")
    .addHeader("Content-Type", "application/json")
    .build()

  return request.sendRequest<QueryResponse>()?.results ?: emptyList()
}



fun getPropertyValue(properties: Map<String, Property>, propertyName: String): String {
  val property = properties[propertyName] ?: return ""
  return when (val type = property.type) {
    "title" -> property.title?.firstOrNull()?.plainText ?: ""
    "rich_text" -> property.richText?.joinToString(separator = "") { it.plainText.orEmpty() } ?: ""
    "select" -> property.select?.name ?: ""
    "multi_select" -> property.multiSelect?.joinToString(", ") { it.name.orEmpty() } ?: ""
    "number" -> property.number?.toString() ?: ""
    "checkbox" -> (property.checkbox ?: false).toString()
    "date" -> property.date?.start ?: ""
    "url" -> property.url ?: ""
    else -> "[${type ?: "unknown"}]"
  }
}

fun parseRow(result: Page): Map<String, String> {
  val row = mutableMapOf<String, String>()
  val properties = result.properties
  for ((key, _) in properties) {
    row[key] = getPropertyValue(properties, key)
  }
  return row
}

fun containsKeyword(result: Page, propertyName: String, keyword: String): Boolean {
  val properties = result.properties
  val value = getPropertyValue(properties, propertyName)
  return value.contains(keyword, ignoreCase = true)
}

// Main execution

val results = queryDatabase(notionDbId)  // Get all rows, sorted by Release
if (results.isEmpty()) {
  println("⚠️ No rows found in database")
}
else {
  var currentReleaseRow: Page? = null
  var nextReleaseRow: Page? = null
  var foundCurrent = false

  for (row in results) {
    val properties = row.properties
    val releaseValue = getPropertyValue(properties, numberColumn)

    if (releaseValue == releaseVersion) {
      currentReleaseRow = row
      foundCurrent = true
      continue
    }
    if (foundCurrent && !containsKeyword(row, skipColumn, "skip")) {
      nextReleaseRow = row
      break
    }
  }

  if (currentReleaseRow != null) {
    val row = parseRow(currentReleaseRow)
    val slackId = row[responsibleSlackColumn].orEmpty()
    println("##teamcity[setParameter name='responsible.slack.id' value='$slackId']")
  }
  else {
    println("⚠️ No non-skipped current release row found!")
  }

  if (nextReleaseRow != null) {
    val nextRow = parseRow(nextReleaseRow)
    val releaseNumber = nextRow[numberColumn].orEmpty()
    println("##teamcity[setParameter name='next.version' value='$releaseNumber']")
  }
  else {
    println("⚠️ No non-skipped next release row found!")
  }
}
