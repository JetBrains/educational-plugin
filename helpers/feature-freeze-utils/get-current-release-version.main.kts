@file:Repository("https://repo.maven.apache.org/maven2/")

import java.io.File
import kotlin.system.exitProcess

val versionParam = args.firstOrNull() ?: run {
  println("Error: VERSION_PARAM is required. Pass the parameter that should be used to store the version as the first argument.\nUsage: kotlinc -script get-current-release-version.main.kts <VERSION_PARAM>")
  exitProcess(1)
}

// Read current version from gradle.properties
val gradlePropertiesFile = File("gradle.properties")
val version = gradlePropertiesFile.readLines().firstOrNull { it.trim().startsWith("pluginVersion=") }?.substringAfter("=")?.trim()

if (version != null) {
  // Set TeamCity parameter using service message
  println("##teamcity[setParameter name='$versionParam' value='$version']")
  println("Plugin version: $version")
}
else {
  System.err.println("Could not determine project version")
  exitProcess(1)
}
