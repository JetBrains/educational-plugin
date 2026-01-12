@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Import("shared-utils.main.kts")

import java.io.File
import kotlin.system.exitProcess

// 1) Read and validate input
val newVersion = args.firstOrNull() ?: run {
  println("Error: NEW_VERSION is required. Pass it as the first argument.\nUsage: kotlinc -script update-master-version.main.kts <NEW_VERSION>")
  exitProcess(1)
}

// 2) Locate gradle.properties in repo root
if (!File("gradle.properties").exists()) {
  error("Error: gradle.properties not found in repo root")
}

// 3) Read current pluginVersion
val currentVersion = getPluginVersion()

if (currentVersion == newVersion) {
  println("pluginVersion is already set to $newVersion, nothing to do")
  exitProcess(0)
}

// 4) Update or append pluginVersion
updatePluginVersion(newVersion)
