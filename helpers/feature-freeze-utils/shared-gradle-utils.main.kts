@file:Repository("https://repo.maven.apache.org/maven2/")

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

  gradlePropertiesFile.writeText(lines.joinToString(System.lineSeparator()))
}
