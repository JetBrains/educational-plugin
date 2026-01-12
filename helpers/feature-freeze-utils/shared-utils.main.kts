@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.17.2")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

// Read version from gradle.properties
fun getPluginVersion(): String? {
  val gradlePropertiesFile = File("gradle.properties")
  return gradlePropertiesFile.readLines().firstOrNull { it.trim().startsWith("pluginVersion=") }?.substringAfter("=")?.trim()
}

fun updatePluginVersion(version: String) {
  val gradlePropertiesFile = File("gradle.properties")

  val lines = gradlePropertiesFile.readLines().toMutableList()

  val pluginVersionLineIndex = lines.indexOfFirst { it.trimStart().startsWith("pluginVersion=") }

  if (pluginVersionLineIndex >= 0) {
    val prefix = lines[pluginVersionLineIndex].substringBefore("=")
    lines[pluginVersionLineIndex] = "$prefix=$version"
  }
  else {
    lines.add("pluginVersion=$version")
  }

  gradlePropertiesFile.writeText(lines.joinToString("\n"))
}

val client = OkHttpClient()
val mapper: ObjectMapper = jacksonObjectMapper()
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

inline fun <reified T> Request.sendRequest(): T? {
  client.newCall(this).execute().use { response ->
    val responseBody = response.body?.string()
    if (!response.isSuccessful) {
      println("❌ Error: ${response.code} - $responseBody")
      error("Failed to fetch data (HTTP ${response.code}): $responseBody")
    }

    return mapper.readValue(responseBody, T::class.java)
  }
}