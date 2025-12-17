@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.system.exitProcess

val NOTION_API_KEY = System.getenv("NOTION_API_KEY")
val NOTION_DB_ID = System.getenv("NOTION_DB_ID")

val releaseVersion: String = args.firstOrNull() ?: run {
    println("Error: RELEASE_VERSION is required. Pass it as the first argument.\nUsage: kotlinc -script update-plugin-version-info.main.kts <RELEASE_VERSION>")
    exitProcess(1)
}

val NUMBER_COLUMN = "Release"
val RESPONSIBLE_SLACK_COLUMN = "Slack ID"
val SKIP_COLUMN = "Note"


if (NOTION_API_KEY.isNullOrEmpty() || NOTION_DB_ID.isNullOrEmpty()) {
    System.err.println("Set NOTION_API_KEY and NOTION_DB_ID first")
    exitProcess(1)
}

val client = OkHttpClient()
val gson = Gson()

fun queryDatabase(databaseId: String): JsonArray? {
    // Query with filter and sorting to ensure consistent order
    val filterBody = """
    {
        "sorts": [
            {
                "property": "$NUMBER_COLUMN",
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
        .addHeader("Authorization", "Bearer $NOTION_API_KEY")
        .addHeader("Notion-Version", "2022-06-28")
        .addHeader("Content-Type", "application/json")
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            println("❌ Error: ${response.code} - ${response.body?.string()}")
            return null
        }
        val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
        return json.getAsJsonArray("results")
    }
}

fun getPropertyValue(properties: JsonObject, propertyName: String): String {
    val property = properties.getAsJsonObject(propertyName) ?: return ""
    val type = property.get("type").asString

    return when (type) {
        "title" -> property.getAsJsonArray("title")
            .firstOrNull()?.asJsonObject?.get("plain_text")?.asString ?: ""
        "rich_text" -> property.getAsJsonArray("rich_text")
            .joinToString("") { it.asJsonObject.get("plain_text").asString }
        "select" -> property.getAsJsonObject("select")?.get("name")?.asString ?: ""
        "multi_select" -> property.getAsJsonArray("multi_select")
            .joinToString(", ") { it.asJsonObject.get("name").asString }
        "number" -> property.get("number")?.toString() ?: ""
        "checkbox" -> property.get("checkbox")?.asBoolean?.toString() ?: "false"
        "date" -> property.getAsJsonObject("date")?.get("start")?.asString ?: ""
        "url" -> property.get("url")?.asString ?: ""
        else -> "[$type]"
    }
}

fun parseRow(result: JsonObject): Map<String, String> {
    val row = mutableMapOf<String, String>()
    val properties = result.getAsJsonObject("properties")

    properties.keySet().forEach { key ->
        row[key] = getPropertyValue(properties, key)
    }
    return row
}

fun containsKeyword(result: JsonObject, propertyName: String, keyword: String): Boolean {
    val properties = result.getAsJsonObject("properties")
    val value = getPropertyValue(properties, propertyName)
    return value.contains(keyword, ignoreCase = true)
}

// Main execution

val results = queryDatabase(NOTION_DB_ID)  // Get all rows, sorted by Release
if (results == null || results.size() == 0) {
    println("⚠️ No rows found in database")
} else {
    var currentReleaseRow: JsonObject? = null
    var nextReleaseRow: JsonObject? = null
    var foundCurrent = false

    for (i in 0 until results.size()) {
        val row = results[i].asJsonObject
        val properties = row.getAsJsonObject("properties")
        val releaseValue = getPropertyValue(properties, NUMBER_COLUMN)

        if (releaseValue == releaseVersion) {
            currentReleaseRow = row
            foundCurrent = true
            continue
        }
        if(foundCurrent && !containsKeyword(row, SKIP_COLUMN, "skip")){
            nextReleaseRow = row
            break
        }
    }

    if (currentReleaseRow != null) {
        val row = parseRow(currentReleaseRow)
        row[RESPONSIBLE_SLACK_COLUMN]?.let {
            println("##teamcity[setParameter name='responsible.slack.id' value='$it']")
        } ?: println("##teamcity[setParameter name='responsible.slack.id' value='']")
    } else {
        println("⚠️ No non-skipped current release row found!")
    }

    if (nextReleaseRow != null) {
        val nextRow = parseRow(nextReleaseRow)
        nextRow[NUMBER_COLUMN]?.let {
            println("##teamcity[setParameter name='next.version' value='$it']")
        } ?: println("##teamcity[setParameter name='next.version' value='']")
    } else {
        println("⚠️ No non-skipped next release row found!")
    }
}
